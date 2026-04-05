package com.example.QuanLyQuanCafe.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.QuanLyQuanCafe.controller.dto.StaffCreateRequest;
import com.example.QuanLyQuanCafe.controller.dto.StaffUpdateRequest;
import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.model.StaffRole;
import com.example.QuanLyQuanCafe.model.StaffStatus;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.repository.StaffRepository;

@Service
public class StaffService {

    private final StaffRepository staffRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffService(StaffRepository staffRepository, AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Staff> findAll() {
        return staffRepository.findAll();
    }

    /**
     * Tải toàn bộ nhân viên và chuyển {@link StaffStatus#ON_LEAVE} → {@link StaffStatus#ACTIVE}
     * khi ngày hiện tại đã sau {@code leaveTo} (hết kỳ phép). Admin có thể đăng ký phép mới sau.
     */
    @Transactional
    public List<Staff> findAllSyncingEndedLeave() {
        List<Staff> list = staffRepository.findAll();
        LocalDate today = LocalDate.now();
        for (Staff s : list) {
            if (s.getStatus() == StaffStatus.ON_LEAVE && s.getLeaveTo() != null && today.isAfter(s.getLeaveTo())) {
                s.setStatus(StaffStatus.ACTIVE);
                normalizeStatusLeaveAndResign(s);
                staffRepository.save(s);
                syncLinkedUserEnabled(s);
            }
        }
        return list;
    }

    /** Cùng quy tắc {@link #findAllSyncingEndedLeave()} cho một bản ghi (chấm công, /me, v.v.). */
    @Transactional
    public Staff syncLeaveEndedToActiveIfNeeded(Long staffId) {
        if (staffId == null) {
            return null;
        }
        return staffRepository.findById(staffId).map(staff -> {
            if (staff.getStatus() != StaffStatus.ON_LEAVE || staff.getLeaveTo() == null) {
                return staff;
            }
            if (!LocalDate.now().isAfter(staff.getLeaveTo())) {
                return staff;
            }
            staff.setStatus(StaffStatus.ACTIVE);
            normalizeStatusLeaveAndResign(staff);
            Staff saved = staffRepository.save(staff);
            syncLinkedUserEnabled(saved);
            return saved;
        }).orElse(null);
    }

    /** Khớp {@link com.example.QuanLyQuanCafe.controller.AttendanceController}: có trong khoảng phép hay không. */
    public boolean isDateInLeavePeriod(Staff staff, LocalDate date) {
        if (staff == null || date == null) {
            return false;
        }
        if (staff.getStatus() != StaffStatus.ON_LEAVE) {
            return false;
        }
        if (staff.getLeaveFrom() == null || staff.getLeaveTo() == null) {
            return false;
        }
        return !date.isBefore(staff.getLeaveFrom()) && !date.isAfter(staff.getLeaveTo());
    }

    /**
     * Trạng thái hiển thị trên thẻ / lọc theo ngày: trước và sau kỳ phép (ON_LEAVE) vẫn coi như đang làm.
     */
    public StaffStatus effectiveCardStatus(Staff staff, LocalDate today) {
        if (staff == null || today == null) {
            return StaffStatus.ACTIVE;
        }
        StaffStatus st = staff.getStatus() != null ? staff.getStatus() : StaffStatus.ACTIVE;
        if (st == StaffStatus.INACTIVE) {
            return StaffStatus.INACTIVE;
        }
        if (st == StaffStatus.ON_LEAVE && isDateInLeavePeriod(staff, today)) {
            return StaffStatus.ON_LEAVE;
        }
        return StaffStatus.ACTIVE;
    }

    public List<Staff> findByStatus(StaffStatus status) {
        return staffRepository.findByStatus(status);
    }

    public List<Staff> findByRole(StaffRole role) {
        return staffRepository.findByRole(role);
    }

    @Transactional
    public Staff create(StaffCreateRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tên nhân viên là bắt buộc");
        }

        Staff staff = new Staff();
        staff.setName(request.getName());

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            staff.setPhone(request.getPhone());
        }

        if (request.getRole() != null) {
            try {
                staff.setRole(StaffRole.valueOf(request.getRole()));
            } catch (IllegalArgumentException ex) {
                // ignore invalid role
            }
        }

        if (request.getStatus() != null) {
            try {
                staff.setStatus(StaffStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException ex) {
                // ignore invalid status
            }
        } else {
            staff.setStatus(StaffStatus.ACTIVE);
        }

        if (request.getShift() != null) {
            staff.setShift(request.getShift());
        }

        if (request.getSalary() != null) {
            staff.setSalary(request.getSalary());
        }

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            staff.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getStartDate() != null) {
            staff.setStartDate(request.getStartDate());
        } else {
            staff.setStartDate(LocalDate.now());
        }

        staff.setLeaveFrom(request.getLeaveFrom());
        staff.setLeaveTo(request.getLeaveTo());
        staff.setLeftOn(request.getLeftOn());
        normalizeStatusLeaveAndResign(staff);

        Staff saved = staffRepository.save(staff);
        createLinkedAppUserForNewStaff(saved, request);
        syncLinkedUserEnabled(saved);
        return saved;
    }

    /**
     * Vai trò Spring Security (chuỗi lưu trong {@link AppUser#getRole()}) theo vị trí nhân viên.
     */
    private static String appRoleForStaffRole(StaffRole r) {
        if (r == null) {
            return "SERVER";
        }
        return switch (r) {
            case PHA_CHE -> "BARISTA";
            case PHUC_VU -> "SERVER";
            case THU_NGAN -> "CASHIER";
            case BAO_VE -> "SECURITY";
            case QUAN_LY -> "CASHIER";
        };
    }

    private void createLinkedAppUserForNewStaff(Staff staff, StaffCreateRequest request) {
        String username = request.getAccountUsername();
        String rawPassword = request.getAccountPassword();
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập username và mật khẩu để tạo tài khoản đăng nhập.");
        }
        username = username.trim();
        if (appUserRepository.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username đã được sử dụng.");
        }
        if (staff.getId() != null && appUserRepository.findByStaffId(staff.getId()) != null) {
            throw new IllegalStateException("Nhân viên này đã có tài khoản liên kết.");
        }
        if (staff.getRole() == null) {
            throw new IllegalArgumentException("Chọn vị trí nhân viên để gán quyền đăng nhập tương ứng.");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(staff.getName());
        if (staff.getPhone() != null && !staff.getPhone().isBlank()) {
            user.setPhone(staff.getPhone().trim());
        }
        if (request.getAccountEmail() != null && !request.getAccountEmail().isBlank()) {
            user.setEmail(request.getAccountEmail().trim());
        }
        user.setRole(appRoleForStaffRole(staff.getRole()));
        user.setStaffId(staff.getId());
        user.setEnabled(staff.getStatus() != StaffStatus.INACTIVE);
        appUserRepository.save(user);
    }

    public Staff update(StaffUpdateRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("Staff id is required");
        }

        Staff staff = staffRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên"));

        if (request.getName() != null) {
            staff.setName(request.getName());
        }

        if (request.getPhone() != null) {
            staff.setPhone(request.getPhone());
        }

        if (request.getRole() != null) {
            try {
                staff.setRole(StaffRole.valueOf(request.getRole()));
            } catch (IllegalArgumentException ex) {
                // ignore invalid role
            }
        }

        if (request.getStatus() != null) {
            try {
                staff.setStatus(StaffStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException ex) {
                // ignore invalid status
            }
        }

        if (request.getShift() != null) {
            staff.setShift(request.getShift());
        }

        if (request.getSalary() != null) {
            staff.setSalary(request.getSalary());
        }

        if (request.getStartDate() != null) {
            staff.setStartDate(request.getStartDate());
        }

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            staff.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getLeaveFrom() != null) {
            staff.setLeaveFrom(request.getLeaveFrom());
        }
        if (request.getLeaveTo() != null) {
            staff.setLeaveTo(request.getLeaveTo());
        }
        if (request.getLeftOn() != null) {
            staff.setLeftOn(request.getLeftOn());
        }

        normalizeStatusLeaveAndResign(staff);

        Staff saved = staffRepository.save(staff);
        syncLinkedUserEnabled(saved);
        syncLinkedAppUserFromStaffUpdate(saved, request);
        return saved;
    }

    private void syncLinkedAppUserFromStaffUpdate(Staff staff, StaffUpdateRequest request) {
        if (staff.getId() == null) {
            return;
        }
        AppUser u = appUserRepository.findByStaffId(staff.getId());
        if (u == null) {
            return;
        }
        u.setFullName(staff.getName());
        u.setPhone(staff.getPhone());
        if (request.getLinkedAccountEmail() != null) {
            String em = request.getLinkedAccountEmail().trim();
            u.setEmail(em.isEmpty() ? null : em);
        }
        if (request.getLinkedAccountNewPassword() != null && !request.getLinkedAccountNewPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(request.getLinkedAccountNewPassword()));
        }
        appUserRepository.save(u);
    }

    /**
     * Theo {@link StaffStatus}: ACTIVE xoá ngày nghỉ/nghỉ việc; ON_LEAVE chuẩn hoá khoảng phép;
     * INACTIVE xoá phép, mặc định {@code leftOn} = hôm nay nếu trống.
     */
    private void normalizeStatusLeaveAndResign(Staff staff) {
        StaffStatus st = staff.getStatus() != null ? staff.getStatus() : StaffStatus.ACTIVE;
        staff.setStatus(st);

        switch (st) {
            case ACTIVE:
                staff.setLeaveFrom(null);
                staff.setLeaveTo(null);
                staff.setLeftOn(null);
                break;
            case ON_LEAVE: {
                staff.setLeftOn(null);
                LocalDate from = staff.getLeaveFrom();
                LocalDate to = staff.getLeaveTo();
                if (from == null && to == null) {
                    throw new IllegalArgumentException("Nghỉ phép cần chọn ít nhất một ngày (từ hoặc đến).");
                }
                if (from == null) {
                    from = to;
                }
                if (to == null) {
                    to = from;
                }
                if (from.isAfter(to)) {
                    throw new IllegalArgumentException("Ngày nghỉ phép không hợp lệ (ngày bắt đầu sau ngày kết thúc).");
                }
                staff.setLeaveFrom(from);
                staff.setLeaveTo(to);
                break;
            }
            case INACTIVE:
                staff.setLeaveFrom(null);
                staff.setLeaveTo(null);
                if (staff.getLeftOn() == null) {
                    staff.setLeftOn(LocalDate.now());
                }
                break;
            default:
                break;
        }
    }

    private void syncLinkedUserEnabled(Staff staff) {
        if (staff.getId() == null) {
            return;
        }
        AppUser user = appUserRepository.findByStaffId(staff.getId());
        if (user == null) {
            return;
        }
        boolean allowLogin = staff.getStatus() != StaffStatus.INACTIVE;
        if (user.isEnabled() != allowLogin) {
            user.setEnabled(allowLogin);
            appUserRepository.save(user);
        }
    }
}

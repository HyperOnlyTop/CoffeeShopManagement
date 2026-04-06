package com.example.QuanLyQuanCafe.config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.QuanLyQuanCafe.model.Attendance;
import com.example.QuanLyQuanCafe.model.AttendanceStatus;
import com.example.QuanLyQuanCafe.model.ShiftCode;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.model.StaffRole;
import com.example.QuanLyQuanCafe.model.StaffShiftAssignment;
import com.example.QuanLyQuanCafe.model.StaffStatus;
import com.example.QuanLyQuanCafe.repository.AttendanceRepository;
import com.example.QuanLyQuanCafe.repository.StaffRepository;
import com.example.QuanLyQuanCafe.repository.StaffShiftAssignmentRepository;
import com.example.QuanLyQuanCafe.service.AttendanceTimeRules;
import com.example.QuanLyQuanCafe.service.AttendanceTimeRules.WorkBlock;

/**
 * Seed phân ca và chấm công cho các ngày 28/3–5/4/2026.
 * <pre>
 * | Vai trò    | Người           | 28/3           | 29/3              | 30/3              | 31/3   | 1/4    | 2/4              | 3/4              | 4/4    | 5/4              |
 * |------------|-----------------|----------------|-------------------|-------------------|--------|--------|------------------|------------------|--------|------------------|
 * | Phục vụ 1  | Trần Văn Khôi   | Sáng           | Chiều             | Tối               | Sáng   | Chiều  | Tối              | Sáng             | Chiều  | Tối              |
 * | Phục vụ 2  | Trần Thị Lan    | Chiều          | Tối               | Sáng              | Chiều  | Tối    | Sáng             | Chiều            | Tối    | Sáng             |
 * | Phục vụ 3  | Lê Văn Hùng     | Tối            | Sáng              | Chiều             | Tối    | Sáng   | Chiều            | Tối              | Sáng   | Chiều            |
 * | Thu ngân 1 | Nguyễn Thị Mai  | Sáng           | Chiều             | Sáng+Chiều (gộp)  | Sáng   | Chiều  | Sáng+Chiều (gộp) | Sáng             | Chiều  | Sáng+Chiều (gộp) |
 * | Thu ngân 2 | Đỗ Minh Tuấn    | Chiều+Tối(gộp) | Sáng+Tối (rời)    | Tối               | C+T    | S+T    | Tối              | Chiều+Tối (gộp)  | S+T    | Tối              |
 * | Pha chế 1  | Lê Minh Đức     | Sáng           | Sáng+Tối (rời)    | Sáng+Chiều (gộp)  | Sáng   | S+T    | Sáng+Chiều (gộp) | Sáng             | S+T    | Sáng+Chiều (gộp) |
 * | Pha chế 2  | Phạm Thu Hà     | Chiều+Tối(gộp) | Chiều             | Tối               | C+T    | Chiều  | Tối              | Chiều+Tối (gộp)  | Chiều  | Tối              |
 * | Bảo vệ     | Phạm Quốc Anh   | Cả ngày        | Cả ngày           | Cả ngày           | Cả ngày| Cả ngày| Cả ngày          | Cả ngày          | Cả ngày| Cả ngày          |
 * </pre>
 * Sau khi phân ca, tự động tạo chấm công (check-in/check-out) với giờ hợp lệ để test tính lương.
 */
@Component
public class ShiftAndAttendanceSeeder {

    private static final Logger log = LoggerFactory.getLogger(ShiftAndAttendanceSeeder.class);

    private final StaffRepository staffRepository;
    private final StaffShiftAssignmentRepository shiftAssignmentRepository;
    private final AttendanceRepository attendanceRepository;

    public ShiftAndAttendanceSeeder(
            StaffRepository staffRepository,
            StaffShiftAssignmentRepository shiftAssignmentRepository,
            AttendanceRepository attendanceRepository) {
        this.staffRepository = staffRepository;
        this.shiftAssignmentRepository = shiftAssignmentRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(250)
    @Transactional
    public void seedShiftsAndAttendance() {
        log.info("ShiftAndAttendanceSeeder: Bắt đầu seed phân ca và chấm công...");
        
        List<LocalDate> dates = List.of(
                LocalDate.of(2026, 3, 28),
                LocalDate.of(2026, 3, 29),
                LocalDate.of(2026, 3, 30),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 2),
                LocalDate.of(2026, 4, 3),
                LocalDate.of(2026, 4, 4),
                LocalDate.of(2026, 4, 5)
        );

        List<Staff> phucVuList = getActiveStaffByRole(StaffRole.PHUC_VU);
        List<Staff> thuNganList = getActiveStaffByRole(StaffRole.THU_NGAN);
        List<Staff> phaChecList = getActiveStaffByRole(StaffRole.PHA_CHE);
        List<Staff> baoVeList = getActiveStaffByRole(StaffRole.BAO_VE);

        log.info("ShiftAndAttendanceSeeder: Tìm thấy {} phục vụ, {} thu ngân, {} pha chế, {} bảo vệ",
                phucVuList.size(), thuNganList.size(), phaChecList.size(), baoVeList.size());

        if (phucVuList.size() < 3) {
            log.warn("ShiftAndAttendanceSeeder: Cần ít nhất 3 phục vụ, hiện có {}. Bỏ qua.", phucVuList.size());
            return;
        }
        if (thuNganList.size() < 2) {
            log.warn("ShiftAndAttendanceSeeder: Cần ít nhất 2 thu ngân, hiện có {}. Bỏ qua.", thuNganList.size());
            return;
        }
        if (phaChecList.size() < 2) {
            log.warn("ShiftAndAttendanceSeeder: Cần ít nhất 2 pha chế, hiện có {}. Bỏ qua.", phaChecList.size());
            return;
        }
        if (baoVeList.isEmpty()) {
            log.warn("ShiftAndAttendanceSeeder: Cần ít nhất 1 bảo vệ. Bỏ qua.");
            return;
        }

        int totalShiftsCreated = 0;
        int totalAttendanceCreated = 0;

        for (int dayIdx = 0; dayIdx < dates.size(); dayIdx++) {
            LocalDate date = dates.get(dayIdx);

            // === PHỤC VỤ: 3 người, mỗi người 1 ca, xoay vòng ===
            ShiftCode[] pvShifts = { ShiftCode.MORNING, ShiftCode.AFTERNOON, ShiftCode.EVENING };
            for (int i = 0; i < 3; i++) {
                Staff pv = phucVuList.get((dayIdx + i) % phucVuList.size());
                ShiftCode shift = pvShifts[i];
                if (createShiftIfNotExists(pv, date, shift)) totalShiftsCreated++;
                if (createAttendanceIfNotExists(pv, date, List.of(shift))) totalAttendanceCreated++;
            }

            // === THU NGÂN: 2 người, phân đều ===
            // Pattern xoay vòng theo ngày:
            // Ngày 0: TN1 = MORNING, TN2 = AFTERNOON + EVENING
            // Ngày 1: TN1 = AFTERNOON, TN2 = MORNING + EVENING
            // Ngày 2: TN1 = EVENING, TN2 = MORNING + AFTERNOON
            Staff tn1 = thuNganList.get(0);
            Staff tn2 = thuNganList.get(1);
            List<ShiftCode> tn1Shifts = getThuNganShiftsForDay(dayIdx, true);
            List<ShiftCode> tn2Shifts = getThuNganShiftsForDay(dayIdx, false);
            for (ShiftCode sc : tn1Shifts) {
                if (createShiftIfNotExists(tn1, date, sc)) totalShiftsCreated++;
            }
            if (createAttendanceIfNotExists(tn1, date, tn1Shifts)) totalAttendanceCreated++;
            for (ShiftCode sc : tn2Shifts) {
                if (createShiftIfNotExists(tn2, date, sc)) totalShiftsCreated++;
            }
            if (createAttendanceIfNotExists(tn2, date, tn2Shifts)) totalAttendanceCreated++;

            // === PHA CHẾ: 2 người, tương tự thu ngân ===
            Staff pc1 = phaChecList.get(0);
            Staff pc2 = phaChecList.get(1);
            List<ShiftCode> pc1Shifts = getPhaChecShiftsForDay(dayIdx, true);
            List<ShiftCode> pc2Shifts = getPhaChecShiftsForDay(dayIdx, false);
            for (ShiftCode sc : pc1Shifts) {
                if (createShiftIfNotExists(pc1, date, sc)) totalShiftsCreated++;
            }
            if (createAttendanceIfNotExists(pc1, date, pc1Shifts)) totalAttendanceCreated++;
            for (ShiftCode sc : pc2Shifts) {
                if (createShiftIfNotExists(pc2, date, sc)) totalShiftsCreated++;
            }
            if (createAttendanceIfNotExists(pc2, date, pc2Shifts)) totalAttendanceCreated++;

            // === BẢO VỆ: FULL_DAY mỗi ngày ===
            Staff bv = baoVeList.get(0);
            if (createShiftIfNotExists(bv, date, ShiftCode.FULL_DAY)) totalShiftsCreated++;
            if (createAttendanceIfNotExists(bv, date, List.of(ShiftCode.FULL_DAY))) totalAttendanceCreated++;
        }

        log.info("ShiftAndAttendanceSeeder: Tạo {} phân ca, {} chấm công cho 28/3–5/4/2026.",
                totalShiftsCreated, totalAttendanceCreated);
    }

    private List<Staff> getActiveStaffByRole(StaffRole role) {
        return staffRepository.findByRole(role).stream()
                .filter(s -> s.getStatus() == StaffStatus.ACTIVE || s.getStatus() == null)
                .toList();
    }

    /**
     * Thu ngân pattern:
     * - Ngày 0 (28/3): TN1 = MORNING, TN2 = AFTERNOON + EVENING (gộp)
     * - Ngày 1 (29/3): TN1 = AFTERNOON, TN2 = MORNING + EVENING (rời)
     * - Ngày 2 (30/3): TN1 = MORNING + AFTERNOON (gộp), TN2 = EVENING
     */
    private List<ShiftCode> getThuNganShiftsForDay(int dayIdx, boolean isPerson1) {
        int pattern = dayIdx % 3;
        if (isPerson1) {
            return switch (pattern) {
                case 0 -> List.of(ShiftCode.MORNING);
                case 1 -> List.of(ShiftCode.AFTERNOON);
                case 2 -> List.of(ShiftCode.MORNING, ShiftCode.AFTERNOON);
                default -> List.of();
            };
        } else {
            return switch (pattern) {
                case 0 -> List.of(ShiftCode.AFTERNOON, ShiftCode.EVENING);
                case 1 -> List.of(ShiftCode.MORNING, ShiftCode.EVENING);
                case 2 -> List.of(ShiftCode.EVENING);
                default -> List.of();
            };
        }
    }

    /**
     * Pha chế pattern:
     * - Ngày 0 (28/3): PC1 = MORNING, PC2 = AFTERNOON + EVENING (gộp)
     * - Ngày 1 (29/3): PC1 = MORNING + EVENING (rời), PC2 = AFTERNOON
     * - Ngày 2 (30/3): PC1 = MORNING + AFTERNOON (gộp), PC2 = EVENING
     */
    private List<ShiftCode> getPhaChecShiftsForDay(int dayIdx, boolean isPerson1) {
        int pattern = dayIdx % 3;
        if (isPerson1) {
            return switch (pattern) {
                case 0 -> List.of(ShiftCode.MORNING);
                case 1 -> List.of(ShiftCode.MORNING, ShiftCode.EVENING);
                case 2 -> List.of(ShiftCode.MORNING, ShiftCode.AFTERNOON);
                default -> List.of();
            };
        } else {
            return switch (pattern) {
                case 0 -> List.of(ShiftCode.AFTERNOON, ShiftCode.EVENING);
                case 1 -> List.of(ShiftCode.AFTERNOON);
                case 2 -> List.of(ShiftCode.EVENING);
                default -> List.of();
            };
        }
    }

    private boolean createShiftIfNotExists(Staff staff, LocalDate date, ShiftCode shiftCode) {
        boolean exists = shiftAssignmentRepository.existsByStaffAndWorkDateAndShiftCode(staff, date, shiftCode);
        if (exists) return false;

        StaffShiftAssignment assignment = new StaffShiftAssignment();
        assignment.setStaff(staff);
        assignment.setWorkDate(date);
        assignment.setShiftCode(shiftCode);
        shiftAssignmentRepository.save(assignment);
        return true;
    }

    /**
     * Tạo chấm công cho staff vào ngày date với các ca shifts.
     * Sử dụng logic gộp ca liền kề (WorkBlock) để tạo attendance đúng.
     */
    private boolean createAttendanceIfNotExists(Staff staff, LocalDate date, List<ShiftCode> shifts) {
        List<Attendance> existing = attendanceRepository.findByStaffAndWorkDate(staff, date);
        if (!existing.isEmpty()) return false;

        List<WorkBlock> blocks = AttendanceTimeRules.mergeAdjacentShifts(new ArrayList<>(shifts));

        for (WorkBlock block : blocks) {
            Attendance attendance = new Attendance();
            attendance.setStaff(staff);
            attendance.setWorkDate(date);
            attendance.setBlockStart(block.getStart());
            attendance.setBlockEnd(block.getEnd());
            attendance.setBlockShifts(String.join(",", block.getShifts().stream().map(ShiftCode::name).toList()));
            attendance.setShiftCode(block.getShifts().get(0));
            attendance.setStatus(AttendanceStatus.PRESENT);

            // Giả lập giờ chấm công hợp lệ (vào đúng giờ, ra đúng giờ hoặc trễ 1-5 phút)
            LocalTime checkIn = block.getStart().plusMinutes(randomMinutes(0, 5));
            LocalTime checkOut = block.getEnd().plusMinutes(randomMinutes(0, 5));

            attendance.setCheckIn(checkIn);
            attendance.setCheckOut(checkOut);

            double workHours = AttendanceTimeRules.computePaidWorkHours(checkIn, checkOut, block);
            attendance.setWorkHours(workHours);

            attendanceRepository.save(attendance);
        }

        return true;
    }

    private int randomMinutes(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }
}

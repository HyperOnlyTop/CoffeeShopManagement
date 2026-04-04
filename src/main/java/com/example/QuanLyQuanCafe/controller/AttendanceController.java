package com.example.QuanLyQuanCafe.controller;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.Attendance;
import com.example.QuanLyQuanCafe.model.AttendanceStatus;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.model.StaffRole;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.repository.AttendanceRepository;
import com.example.QuanLyQuanCafe.repository.StaffRepository;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final StaffRepository staffRepository;
    private final AppUserRepository appUserRepository;

    public AttendanceController(AttendanceRepository attendanceRepository, StaffRepository staffRepository, AppUserRepository appUserRepository) {
        this.attendanceRepository = attendanceRepository;
        this.staffRepository = staffRepository;
        this.appUserRepository = appUserRepository;
    }

    private Staff resolveStaff(Principal principal) {
        if (principal == null) return null;
        AppUser user = appUserRepository.findByUsername(principal.getName());
        if (user != null && user.getStaffId() != null) {
            return staffRepository.findById(user.getStaffId()).orElse(null);
        }
        // fallback legacy
        return staffRepository.findByName(principal.getName());
    }

    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(Principal principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().body("Not authenticated");
        }

        String username = principal.getName();
        Staff staff = resolveStaff(principal);
        
        if (staff == null) {
            staff = new Staff();
            staff.setName(username);
            // Default role fallback if parsing fails, but use PHUC_VU for now
            staff.setRole(StaffRole.PHUC_VU);
            staff = staffRepository.save(staff);
        }

        LocalDate today = LocalDate.now();
        List<Attendance> current = attendanceRepository.findByStaffAndWorkDateBetween(staff, today, today);
        if (!current.isEmpty()) {
            return ResponseEntity.badRequest().body("Bạn đã chấm công ngày hôm nay rồi.");
        }

        Attendance attendance = new Attendance();
        attendance.setStaff(staff);
        attendance.setWorkDate(today);
        attendance.setCheckIn(LocalTime.now());
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(attendance);

        return ResponseEntity.ok("Chấm công thành công! Bắt đầu tính giờ.");
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayStatus(Principal principal) {
        if (principal == null) return ResponseEntity.badRequest().body(Map.of("error", "Not authenticated"));
        Staff staff = resolveStaff(principal);
        if (staff == null) return ResponseEntity.ok(Map.of("status", "NOT_STAFF"));

        LocalDate today = LocalDate.now();
        List<Attendance> current = attendanceRepository.findByStaffAndWorkDateBetween(staff, today, today);
        if (current.isEmpty()) return ResponseEntity.ok(Map.of("status", "NOT_CHECKED_IN"));
        
        Attendance att = current.get(0);
        if (att.getCheckOut() == null) {
            return ResponseEntity.ok(Map.of("status", "CHECKED_IN", "checkInTime", att.getCheckIn().toString()));
        } else {
            return ResponseEntity.ok(Map.of("status", "COMPLETED", "workHours", att.getWorkHours() != null ? att.getWorkHours() : 0.0));
        }
    }

    @PostMapping("/check-out")
    public ResponseEntity<?> checkOut(Principal principal) {
        if (principal == null) return ResponseEntity.badRequest().body("Not authenticated");
        Staff staff = resolveStaff(principal);
        if (staff == null) return ResponseEntity.badRequest().body("Không tìm thấy nhân viên");

        LocalDate today = LocalDate.now();
        List<Attendance> current = attendanceRepository.findByStaffAndWorkDateBetween(staff, today, today);
        if (current.isEmpty()) return ResponseEntity.badRequest().body("Bạn chưa chấm công (Giờ vào) hôm nay.");

        Attendance att = current.get(0);
        if (att.getCheckOut() != null) return ResponseEntity.badRequest().body("Bạn đã chấm công (Giờ ra) rồi.");

        LocalTime now = LocalTime.now();
        att.setCheckOut(now);
        
        long minutes = Duration.between(att.getCheckIn(), now).toMinutes();
        double hours = (double) minutes / 60.0;
        hours = Math.round(hours * 100.0) / 100.0;
        att.setWorkHours(hours);
        
        attendanceRepository.save(att);
        return ResponseEntity.ok("Chấm công ra thành công! Tổng giờ làm: " + hours + " giờ");
    }

    /** Lịch sử chấm công của chính người đăng nhập (theo khoảng ngày). */
    @GetMapping("/my-records")
    public ResponseEntity<?> myRecords(
            Principal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Staff staff = resolveStaff(principal);
        if (staff == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Chưa có hồ sơ nhân viên liên kết."));
        }
        if (from == null || to == null || from.isAfter(to)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Khoảng ngày không hợp lệ"));
        }

        List<Attendance> list = attendanceRepository.findByStaffAndWorkDateBetween(staff, from, to);
        list.sort(Comparator.comparing(Attendance::getWorkDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        List<Map<String, Object>> rows = list.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("workDate", a.getWorkDate());
            m.put("checkIn", a.getCheckIn() != null ? a.getCheckIn().toString() : null);
            m.put("checkOut", a.getCheckOut() != null ? a.getCheckOut().toString() : null);
            m.put("workHours", a.getWorkHours());
            m.put("status", a.getStatus() != null ? a.getStatus().name() : null);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(rows);
    }

    /** Báo cáo chấm công theo khoảng ngày — chỉ ADMIN (xem SecurityConfig). */
    @GetMapping("/report")
    public ResponseEntity<?> attendanceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (from == null || to == null || from.isAfter(to)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Khoảng ngày không hợp lệ"));
        }

        List<Map<String, Object>> rows = attendanceRepository.findReportByWorkDateRange(from, to).stream()
                .map(a -> {
                    Staff s = a.getStaff();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("staffId", s != null ? s.getId() : null);
                    m.put("staffName", s != null ? s.getName() : null);
                    m.put("workDate", a.getWorkDate());
                    m.put("checkIn", a.getCheckIn() != null ? a.getCheckIn().toString() : null);
                    m.put("checkOut", a.getCheckOut() != null ? a.getCheckOut().toString() : null);
                    m.put("workHours", a.getWorkHours());
                    m.put("status", a.getStatus() != null ? a.getStatus().name() : null);
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(rows);
    }
}

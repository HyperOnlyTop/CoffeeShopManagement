package com.example.QuanLyQuanCafe.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.controller.dto.StaffCreateRequest;
import com.example.QuanLyQuanCafe.controller.dto.StaffUpdateRequest;
import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.Attendance;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.model.StaffRole;
import com.example.QuanLyQuanCafe.model.StaffStatus;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.repository.StaffRepository;
import com.example.QuanLyQuanCafe.service.AttendanceService;
import com.example.QuanLyQuanCafe.service.StaffService;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;
    private final StaffRepository staffRepository;
    private final AppUserRepository appUserRepository;
    private final AttendanceService attendanceService;

    public StaffController(StaffService staffService,
                          StaffRepository staffRepository,
                          AppUserRepository appUserRepository,
                          AttendanceService attendanceService) {
        this.staffService = staffService;
        this.staffRepository = staffRepository;
        this.appUserRepository = appUserRepository;
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public ResponseEntity<List<Staff>> getAll() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(staffService.findAll());
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Staff> getMe(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AppUser appUser = appUserRepository.findByUsername(principal.getName());
        if (appUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Staff staff = null;
        if (appUser.getFullName() != null && !appUser.getFullName().isBlank()) {
            staff = staffRepository.findByName(appUser.getFullName());
        }
        if (staff == null) {
            staff = staffRepository.findByName(principal.getName());
        }
        if (staff == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(staff);
    }

    @GetMapping("/me/salary")
    public ResponseEntity<?> getMySalary() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        AppUser appUser = appUserRepository.findByUsername(username);

        Staff staff = null;
        if (appUser != null && appUser.getFullName() != null && !appUser.getFullName().isBlank()) {
            staff = staffRepository.findByName(appUser.getFullName());
        }
        if (staff == null) {
            staff = staffRepository.findByName(username);
        }
        if (staff == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Không tìm thấy nhân viên"));
        }

        LocalDate today = LocalDate.now();
        LocalDate from = today.withDayOfMonth(1);
        LocalDate to = today;

        List<Attendance> records = attendanceService.findByStaffAndDateRange(staff, from, to);
        double totalHours = records.stream()
                .mapToDouble(a -> a.getWorkHours() != null ? a.getWorkHours() : 0.0)
                .sum();

        BigDecimal hourlyRate = staff.getSalary() != null ? staff.getSalary() : BigDecimal.ZERO;
        BigDecimal totalSalary = hourlyRate
                .multiply(BigDecimal.valueOf(totalHours))
                .setScale(0, RoundingMode.HALF_UP);

        return ResponseEntity.ok(Map.of(
                "staffId", staff.getId(),
                "totalHours", totalHours,
                "hourlyRate", hourlyRate,
                "totalSalary", totalSalary,
                "fromDate", from,
                "toDate", to
        ));
    }

    @GetMapping("/status/{status}")
    public List<Staff> getByStatus(@PathVariable("status") StaffStatus status) {
        return staffService.findByStatus(status);
    }

    @GetMapping("/role/{role}")
    public List<Staff> getByRole(@PathVariable("role") StaffRole role) {
        return staffService.findByRole(role);
    }

    @GetMapping("/{id}/salary")
    public ResponseEntity<?> getSalarySummary(
            @PathVariable("id") Long id,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Staff staff = staffRepository.findById(id).orElse(null);
        if (staff == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Không tìm thấy nhân viên"));
        }

        LocalDate today = LocalDate.now();
        if (from == null) {
            from = today.withDayOfMonth(1);
        }
        if (to == null) {
            to = today;
        }

        List<Attendance> records = attendanceService.findByStaffAndDateRange(staff, from, to);
        double totalHours = records.stream()
                .mapToDouble(a -> a.getWorkHours() != null ? a.getWorkHours() : 0.0)
                .sum();

        BigDecimal hourlyRate = staff.getSalary() != null ? staff.getSalary() : BigDecimal.ZERO;
        BigDecimal totalSalary = hourlyRate
                .multiply(BigDecimal.valueOf(totalHours))
                .setScale(0, RoundingMode.HALF_UP);

        return ResponseEntity.ok(Map.of(
                "staffId", staff.getId(),
                "totalHours", totalHours,
                "hourlyRate", hourlyRate,
                "totalSalary", totalSalary,
                "fromDate", from,
                "toDate", to
        ));
    }

    @PostMapping
    public Staff create(@RequestBody StaffCreateRequest request) {
        return staffService.create(request);
    }

    @PostMapping("/update")
    public Staff update(@RequestBody StaffUpdateRequest request) {
        return staffService.update(request);
    }
}

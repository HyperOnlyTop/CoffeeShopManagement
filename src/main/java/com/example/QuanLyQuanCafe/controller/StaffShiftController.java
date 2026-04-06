package com.example.QuanLyQuanCafe.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.controller.dto.ShiftAssignRequest;
import com.example.QuanLyQuanCafe.model.ShiftCode;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.model.StaffStatus;
import com.example.QuanLyQuanCafe.model.StaffShiftAssignment;
import com.example.QuanLyQuanCafe.repository.StaffRepository;
import com.example.QuanLyQuanCafe.repository.StaffShiftAssignmentRepository;
import com.example.QuanLyQuanCafe.service.StaffService;

@RestController
@RequestMapping("/api/staff-shifts")
public class StaffShiftController {
    private final StaffShiftAssignmentRepository repo;
    private final StaffRepository staffRepository;
    private final StaffService staffService;

    public StaffShiftController(StaffShiftAssignmentRepository repo, StaffRepository staffRepository,
            StaffService staffService) {
        this.repo = repo;
        this.staffRepository = staffRepository;
        this.staffService = staffService;
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam("date") String date) {
        LocalDate d;
        try {
            d = LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date"));
        }
        List<StaffShiftAssignment> list = repo.findByWorkDate(d);
        // Tránh lỗi serialize Hibernate proxy (staff lazy). Trả JSON tối giản cho UI.
        List<Map<String, Object>> res = list.stream()
                .map(a -> Map.of(
                        "staff", Map.of("id", a.getStaff() != null ? a.getStaff().getId() : null),
                        "shiftCode", a.getShiftCode() != null ? a.getShiftCode().name() : null,
                        "workDate", a.getWorkDate() != null ? a.getWorkDate().toString() : null
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/assign")
    public ResponseEntity<?> assign(@RequestBody ShiftAssignRequest req) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (req == null || req.getStaffId() == null || req.getWorkDate() == null || req.getShiftCode() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing fields"));
        }
        Staff staff = staffRepository.findById(req.getStaffId()).orElse(null);
        if (staff == null) return ResponseEntity.badRequest().body(Map.of("error", "Staff not found"));
        staff = staffService.syncLeaveEndedToActiveIfNeeded(staff.getId());
        if (staff == null) return ResponseEntity.badRequest().body(Map.of("error", "Staff not found"));
        LocalDate d;
        try {
            d = LocalDate.parse(req.getWorkDate());
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date"));
        }
        if (staff.getStatus() == StaffStatus.INACTIVE) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nhân viên đã nghỉ việc, không gán ca."));
        }
        if (staff.getStatus() == StaffStatus.ON_LEAVE
                && staff.getLeaveFrom() != null
                && staff.getLeaveTo() != null
                && !d.isBefore(staff.getLeaveFrom())
                && !d.isAfter(staff.getLeaveTo())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nhân viên đang nghỉ phép trong ngày này."));
        }
        ShiftCode code;
        try {
            code = ShiftCode.valueOf(req.getShiftCode());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid shift"));
        }

        // Rule B: FULL_DAY không được chồng với các ca còn lại trong cùng ngày
        if (code == ShiftCode.FULL_DAY) {
            boolean hasOther = repo.existsByStaffAndWorkDateAndShiftCode(staff, d, ShiftCode.MORNING)
                    || repo.existsByStaffAndWorkDateAndShiftCode(staff, d, ShiftCode.AFTERNOON)
                    || repo.existsByStaffAndWorkDateAndShiftCode(staff, d, ShiftCode.EVENING);
            if (hasOther) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không thể gán CA CẢ NGÀY khi đã có ca khác trong ngày"));
            }
        } else {
            boolean hasFullDay = repo.existsByStaffAndWorkDateAndShiftCode(staff, d, ShiftCode.FULL_DAY);
            if (hasFullDay) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không thể gán ca này khi đã có CA CẢ NGÀY trong ngày"));
            }
        }

        if (repo.existsByStaffAndWorkDateAndShiftCode(staff, d, code)) {
            return ResponseEntity.ok(Map.of("ok", true, "exists", true));
        }
        StaffShiftAssignment a = new StaffShiftAssignment();
        a.setStaff(staff);
        a.setWorkDate(d);
        a.setShiftCode(code);
        repo.save(a);

        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/unassign")
    @Transactional
    public ResponseEntity<?> unassign(@RequestBody ShiftAssignRequest req) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (req == null || req.getStaffId() == null || req.getWorkDate() == null || req.getShiftCode() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing fields"));
        }
        Staff staff = staffRepository.findById(req.getStaffId()).orElse(null);
        if (staff == null) return ResponseEntity.badRequest().body(Map.of("error", "Staff not found"));
        LocalDate d;
        try {
            d = LocalDate.parse(req.getWorkDate());
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date"));
        }
        ShiftCode code;
        try {
            code = ShiftCode.valueOf(req.getShiftCode());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid shift"));
        }
        repo.deleteByStaffAndWorkDateAndShiftCode(staff, d, code);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/templates")
    public ResponseEntity<?> templates() {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Map<String, Object> res = new HashMap<>();
        for (ShiftCode c : ShiftCode.values()) {
            res.put(c.name(), Map.of("start", c.getStart().toString(), "end", c.getEnd().toString()));
        }
        return ResponseEntity.ok(res);
    }
}


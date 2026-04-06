package com.example.QuanLyQuanCafe.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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
import com.example.QuanLyQuanCafe.model.ShiftCode;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.model.StaffShiftAssignment;
import com.example.QuanLyQuanCafe.model.StaffStatus;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.repository.AttendanceRepository;
import com.example.QuanLyQuanCafe.repository.StaffRepository;
import com.example.QuanLyQuanCafe.repository.StaffShiftAssignmentRepository;
import com.example.QuanLyQuanCafe.service.AttendanceTimeRules;
import com.example.QuanLyQuanCafe.service.AttendanceTimeRules.WorkBlock;
import com.example.QuanLyQuanCafe.service.StaffService;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final StaffRepository staffRepository;
    private final AppUserRepository appUserRepository;
    private final StaffShiftAssignmentRepository shiftAssignmentRepository;
    private final StaffService staffService;

    public AttendanceController(AttendanceRepository attendanceRepository, StaffRepository staffRepository,
            AppUserRepository appUserRepository, StaffShiftAssignmentRepository shiftAssignmentRepository,
            StaffService staffService) {
        this.attendanceRepository = attendanceRepository;
        this.staffRepository = staffRepository;
        this.appUserRepository = appUserRepository;
        this.shiftAssignmentRepository = shiftAssignmentRepository;
        this.staffService = staffService;
    }

    /** Hết kỳ phép (sau leaveTo) → ACTIVE trong DB trước khi chấm công / đọc trạng thái. */
    private Staff withLeaveEndedSync(Staff staff) {
        if (staff == null || staff.getId() == null) {
            return staff;
        }
        Staff synced = staffService.syncLeaveEndedToActiveIfNeeded(staff.getId());
        return synced != null ? synced : staff;
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

    /** Chặn chấm công khi đã nghỉ việc hoặc đang trong kỳ nghỉ phép (nếu đã khai báo khoảng ngày). */
    private String attendanceBlockReason(Staff staff, LocalDate workDate) {
        if (staff == null) {
            return null;
        }
        if (staff.getStatus() == StaffStatus.INACTIVE) {
            return "Nhân viên đã nghỉ việc, không thể chấm công.";
        }
        if (staff.getStatus() == StaffStatus.ON_LEAVE
                && staff.getLeaveFrom() != null
                && staff.getLeaveTo() != null
                && !workDate.isBefore(staff.getLeaveFrom())
                && !workDate.isAfter(staff.getLeaveTo())) {
            return "Bạn đang trong kỳ nghỉ phép, không chấm công trong khoảng này.";
        }
        return null;
    }

    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(Principal principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().body("Not authenticated");
        }

        Staff staff = withLeaveEndedSync(resolveStaff(principal));
        if (staff == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy hồ sơ nhân viên. Liên hệ quản lý.");
        }

        LocalDate today = LocalDate.now();
        String block = attendanceBlockReason(staff, today);
        if (block != null) {
            return ResponseEntity.badRequest().body(block);
        }

        LocalTime now = LocalTime.now();
        String globalBlock = AttendanceTimeRules.globalPunchWindowBlockReason(now);
        if (globalBlock != null) {
            return ResponseEntity.badRequest().body(globalBlock);
        }

        List<StaffShiftAssignment> todayShifts = shiftAssignmentRepository.findByStaffAndWorkDate(staff, today);
        if (todayShifts.isEmpty()) {
            return ResponseEntity.badRequest().body("Bạn chưa được phân ca hôm nay. Liên hệ quản lý.");
        }

        List<Attendance> todayAttendances = attendanceRepository.findByStaffAndWorkDate(staff, today);
        Attendance openSegment = todayAttendances.stream()
                .filter(a -> a.getCheckOut() == null)
                .findFirst().orElse(null);
        if (openSegment != null) {
            String openLabel = openSegment.getBlockShifts() != null ? openSegment.getBlockShifts() : shiftLabel(openSegment.getShiftCode());
            return ResponseEntity.badRequest().body("Bạn đang có ca chưa chấm ra (" + openLabel + "). Hãy chấm ra trước.");
        }

        // Gộp ca liền kề thành các WorkBlock
        List<ShiftCode> assignedShiftCodes = todayShifts.stream()
                .map(StaffShiftAssignment::getShiftCode)
                .collect(Collectors.toList());
        List<WorkBlock> workBlocks = AttendanceTimeRules.mergeAdjacentShifts(assignedShiftCodes);

        // Lọc các block chưa chấm (không có attendance nào cover toàn bộ block đó)
        List<WorkBlock> remainingBlocks = workBlocks.stream()
                .filter(wb -> !isBlockAlreadyAttended(wb, todayAttendances))
                .collect(Collectors.toList());

        // Tìm block đang trong cửa sổ chấm vào
        WorkBlock targetBlock = remainingBlocks.stream()
                .filter(wb -> AttendanceTimeRules.isWithinPunchInWindow(now, wb))
                .min(Comparator.comparing(WorkBlock::getStart))
                .orElse(null);

        if (targetBlock == null) {
            if (remainingBlocks.isEmpty()) {
                return ResponseEntity.badRequest().body("Bạn đã chấm đủ tất cả ca hôm nay.");
            }
            WorkBlock nextBlock = remainingBlocks.stream()
                    .min(Comparator.comparing(WorkBlock::getStart))
                    .orElse(null);
            if (nextBlock != null && now.isBefore(AttendanceTimeRules.punchInWindowStart(nextBlock))) {
                return ResponseEntity.badRequest().body("Chưa đến giờ chấm công. Ca tiếp theo: " + nextBlock.getLabel() + ", mở từ " + AttendanceTimeRules.punchInWindowStart(nextBlock) + ".");
            }
            return ResponseEntity.badRequest().body("Không có ca nào trong cửa sổ chấm vào lúc này.");
        }

        String blockReason = AttendanceTimeRules.punchInBlockReason(now, targetBlock);
        if (blockReason != null) {
            return ResponseEntity.badRequest().body(blockReason);
        }

        Attendance attendance = new Attendance();
        attendance.setStaff(staff);
        attendance.setWorkDate(today);
        attendance.setCheckIn(now);
        attendance.setBlockStart(targetBlock.getStart());
        attendance.setBlockEnd(targetBlock.getEnd());
        attendance.setBlockShifts(targetBlock.getShifts().stream().map(ShiftCode::name).collect(Collectors.joining(",")));
        // Lưu shiftCode đầu tiên cho backward compatibility
        attendance.setShiftCode(targetBlock.getShifts().get(0));
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(attendance);

        return ResponseEntity.ok("Chấm công vào " + targetBlock.getLabel() + " thành công!");
    }

    /** Kiểm tra block đã có attendance cover chưa (dựa trên blockStart/blockEnd hoặc shiftCode). */
    private boolean isBlockAlreadyAttended(WorkBlock block, List<Attendance> attendances) {
        for (Attendance a : attendances) {
            // Nếu có blockStart/blockEnd, so sánh chính xác
            if (a.getBlockStart() != null && a.getBlockEnd() != null) {
                if (a.getBlockStart().equals(block.getStart()) && a.getBlockEnd().equals(block.getEnd())) {
                    return true;
                }
            } else if (a.getShiftCode() != null) {
                // Fallback: so sánh theo shiftCode (dữ liệu cũ hoặc ca đơn)
                if (block.getShifts().size() == 1 && block.getShifts().contains(a.getShiftCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String shiftLabel(ShiftCode sc) {
        if (sc == null) return "không xác định";
        return switch (sc) {
            case MORNING -> "sáng";
            case AFTERNOON -> "chiều";
            case EVENING -> "tối";
            case FULL_DAY -> "cả ngày";
        };
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayStatus(Principal principal) {
        if (principal == null) return ResponseEntity.badRequest().body(Map.of("error", "Not authenticated"));
        Staff staff = withLeaveEndedSync(resolveStaff(principal));
        if (staff == null) return ResponseEntity.ok(Map.of("status", "NOT_STAFF"));

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<StaffShiftAssignment> todayShifts = shiftAssignmentRepository.findByStaffAndWorkDate(staff, today);
        List<Attendance> todayAttendances = attendanceRepository.findByStaffAndWorkDate(staff, today);

        Attendance openSegment = todayAttendances.stream()
                .filter(a -> a.getCheckOut() == null)
                .findFirst().orElse(null);

        double totalHoursToday = todayAttendances.stream()
                .filter(a -> a.getWorkHours() != null)
                .mapToDouble(Attendance::getWorkHours)
                .sum();
        totalHoursToday = Math.round(totalHoursToday * 100.0) / 100.0;

        // Lấy danh sách tất cả ca đã hoàn thành (từ blockShifts hoặc shiftCode)
        List<String> completedShiftNames = new ArrayList<>();
        for (Attendance a : todayAttendances) {
            if (a.getCheckOut() != null) {
                if (a.getBlockShifts() != null && !a.getBlockShifts().isEmpty()) {
                    for (String s : a.getBlockShifts().split(",")) {
                        completedShiftNames.add(s.trim());
                    }
                } else if (a.getShiftCode() != null) {
                    completedShiftNames.add(a.getShiftCode().name());
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalHoursToday", totalHoursToday);
        result.put("completedShifts", completedShiftNames);

        if (todayShifts.isEmpty()) {
            result.put("status", "NO_SHIFT");
            result.put("message", "Chưa được phân ca hôm nay.");
            return ResponseEntity.ok(result);
        }

        if (openSegment != null) {
            result.put("status", "CHECKED_IN");
            result.put("checkInTime", openSegment.getCheckIn().toString());
            String currentBlockLabel = openSegment.getBlockShifts() != null ? openSegment.getBlockShifts() : (openSegment.getShiftCode() != null ? openSegment.getShiftCode().name() : null);
            result.put("currentBlock", currentBlockLabel);
            result.put("currentBlockStart", openSegment.getBlockStart() != null ? openSegment.getBlockStart().toString() : null);
            result.put("currentBlockEnd", openSegment.getBlockEnd() != null ? openSegment.getBlockEnd().toString() : null);
            return ResponseEntity.ok(result);
        }

        // Gộp ca liền kề thành WorkBlock
        List<ShiftCode> assignedShiftCodes = todayShifts.stream()
                .map(StaffShiftAssignment::getShiftCode)
                .collect(Collectors.toList());
        List<WorkBlock> workBlocks = AttendanceTimeRules.mergeAdjacentShifts(assignedShiftCodes);

        // Lọc các block chưa chấm
        List<WorkBlock> remainingBlocks = workBlocks.stream()
                .filter(wb -> !isBlockAlreadyAttended(wb, todayAttendances))
                .collect(Collectors.toList());

        if (remainingBlocks.isEmpty()) {
            result.put("status", "ALL_COMPLETED");
            result.put("message", "Đã chấm đủ tất cả ca hôm nay.");
            return ResponseEntity.ok(result);
        }

        WorkBlock nextBlock = remainingBlocks.stream()
                .min(Comparator.comparing(WorkBlock::getStart))
                .orElse(null);

        if (nextBlock != null) {
            result.put("nextBlock", nextBlock.getLabel());
            result.put("nextBlockShifts", nextBlock.getShifts().stream().map(ShiftCode::name).collect(Collectors.toList()));
            result.put("nextBlockStart", nextBlock.getStart().toString());
            result.put("nextBlockEnd", nextBlock.getEnd().toString());

            boolean canCheckInNow = AttendanceTimeRules.isWithinPunchInWindow(now, nextBlock);
            result.put("canCheckInNow", canCheckInNow);

            if (canCheckInNow) {
                result.put("status", "READY_TO_CHECK_IN");
                result.put("message", "Sẵn sàng chấm công vào " + nextBlock.getLabel() + ".");
            } else if (now.isBefore(AttendanceTimeRules.punchInWindowStart(nextBlock))) {
                result.put("status", "WAITING_FOR_SHIFT");
                result.put("message", "Chờ đến " + AttendanceTimeRules.punchInWindowStart(nextBlock) + " để chấm công " + nextBlock.getLabel() + ".");
            } else {
                result.put("status", "SHIFT_WINDOW_PASSED");
                result.put("message", "Đã quá giờ chấm vào " + nextBlock.getLabel() + ". Liên hệ quản lý.");
            }
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/check-out")
    public ResponseEntity<?> checkOut(Principal principal) {
        if (principal == null) return ResponseEntity.badRequest().body("Not authenticated");
        Staff staff = withLeaveEndedSync(resolveStaff(principal));
        if (staff == null) return ResponseEntity.badRequest().body("Không tìm thấy nhân viên");

        LocalDate today = LocalDate.now();
        String block = attendanceBlockReason(staff, today);
        if (block != null) {
            return ResponseEntity.badRequest().body(block);
        }

        LocalTime now = LocalTime.now();
        String globalBlock = AttendanceTimeRules.globalPunchWindowBlockReason(now);
        if (globalBlock != null) {
            return ResponseEntity.badRequest().body(globalBlock);
        }

        List<Attendance> todayAttendances = attendanceRepository.findByStaffAndWorkDate(staff, today);
        Attendance openSegment = todayAttendances.stream()
                .filter(a -> a.getCheckOut() == null)
                .findFirst().orElse(null);
        if (openSegment == null) {
            return ResponseEntity.badRequest().body("Bạn chưa chấm công vào hôm nay hoặc đã chấm ra hết các ca.");
        }

        // Tạo WorkBlock từ attendance record để kiểm tra cửa sổ ra và tính lương
        WorkBlock workBlock = buildWorkBlockFromAttendance(openSegment);
        if (workBlock != null) {
            String blockReason = AttendanceTimeRules.punchOutBlockReason(now, workBlock);
            if (blockReason != null) {
                return ResponseEntity.badRequest().body(blockReason);
            }
        }

        openSegment.setCheckOut(now);
        double hours;
        if (workBlock != null) {
            hours = AttendanceTimeRules.computePaidWorkHours(openSegment.getCheckIn(), now, workBlock);
        } else if (openSegment.getShiftCode() != null) {
            hours = AttendanceTimeRules.computePaidWorkHours(openSegment.getCheckIn(), now, openSegment.getShiftCode());
        } else {
            hours = AttendanceTimeRules.computePaidWorkHours(openSegment.getCheckIn(), now);
        }
        openSegment.setWorkHours(hours);

        attendanceRepository.save(openSegment);
        String blockLabel = (workBlock != null) ? " " + workBlock.getLabel() : (openSegment.getShiftCode() != null ? " ca " + shiftLabel(openSegment.getShiftCode()) : "");
        return ResponseEntity.ok("Chấm công ra" + blockLabel + " thành công! Giờ làm: " + hours + " giờ");
    }

    /** Tạo WorkBlock từ attendance record (dựa trên blockStart/blockEnd hoặc shiftCode). */
    private WorkBlock buildWorkBlockFromAttendance(Attendance a) {
        if (a.getBlockStart() != null && a.getBlockEnd() != null) {
            List<ShiftCode> shifts = new ArrayList<>();
            if (a.getBlockShifts() != null && !a.getBlockShifts().isEmpty()) {
                for (String s : a.getBlockShifts().split(",")) {
                    try {
                        shifts.add(ShiftCode.valueOf(s.trim()));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            if (shifts.isEmpty() && a.getShiftCode() != null) {
                shifts.add(a.getShiftCode());
            }
            return new WorkBlock(a.getBlockStart(), a.getBlockEnd(), shifts);
        } else if (a.getShiftCode() != null) {
            return new WorkBlock(a.getShiftCode().getStart(), a.getShiftCode().getEnd(), List.of(a.getShiftCode()));
        }
        return null;
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
        Staff staff = withLeaveEndedSync(resolveStaff(principal));
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
            m.put("shiftCode", a.getShiftCode() != null ? a.getShiftCode().name() : null);
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
                    m.put("shiftCode", a.getShiftCode() != null ? a.getShiftCode().name() : null);
                    m.put("status", a.getStatus() != null ? a.getStatus().name() : null);
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(rows);
    }
}

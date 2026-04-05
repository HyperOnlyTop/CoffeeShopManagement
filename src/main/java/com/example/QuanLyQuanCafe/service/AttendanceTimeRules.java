package com.example.QuanLyQuanCafe.service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.QuanLyQuanCafe.model.ShiftCode;

/**
 * Quy tắc chấm công và tính giờ trả lương theo ca.
 * <ul>
 *   <li>Ca liền kề (sáng+chiều, chiều+tối, sáng+chiều+tối): gộp thành 1 segment, vào 1 lần ra 1 lần.</li>
 *   <li>Ca rời (sáng+tối): vào/ra riêng biệt cho mỗi ca.</li>
 *   <li>Cửa sổ chấm vào: từ (đầu khung − 15p) đến (cuối khung − 15p).</li>
 *   <li>Cửa sổ chấm ra: từ (đầu khung + 15p) đến (cuối khung + 15p).</li>
 *   <li>Giờ tính lương: clamp trong [đầu khung, cuối khung].</li>
 * </ul>
 */
public final class AttendanceTimeRules {

    /** Buffer mở trước đầu ca / sau cuối ca cho phép bấm. */
    public static final int PUNCH_BUFFER_MINUTES = 15;

    /** Trần an toàn toàn quán (legacy / fallback). */
    public static final LocalTime PUNCH_OPEN = LocalTime.of(6, 30);
    public static final LocalTime PUNCH_CLOSE = LocalTime.of(22, 30);

    /** Khung tính lương mặc định cả ngày (dùng khi không có ca cụ thể). */
    public static final LocalTime PAY_DAY_START = LocalTime.of(7, 0);
    public static final LocalTime PAY_DAY_END = LocalTime.of(22, 0);

    private AttendanceTimeRules() {
    }

    /** Cửa sổ cho phép chấm VÀO ca: [shiftStart − 15p, shiftEnd − 15p]. */
    public static LocalTime punchInWindowStart(ShiftCode shift) {
        return shift.getStart().minusMinutes(PUNCH_BUFFER_MINUTES);
    }

    public static LocalTime punchInWindowEnd(ShiftCode shift) {
        return shift.getEnd().minusMinutes(PUNCH_BUFFER_MINUTES);
    }

    /** Cửa sổ cho phép chấm RA ca: [shiftStart + 15p, shiftEnd + 15p]. */
    public static LocalTime punchOutWindowStart(ShiftCode shift) {
        return shift.getStart().plusMinutes(PUNCH_BUFFER_MINUTES);
    }

    public static LocalTime punchOutWindowEnd(ShiftCode shift) {
        return shift.getEnd().plusMinutes(PUNCH_BUFFER_MINUTES);
    }

    /** Kiểm tra giờ hiện tại có trong cửa sổ chấm VÀO của ca không. */
    public static boolean isWithinPunchInWindow(LocalTime now, ShiftCode shift) {
        if (now == null || shift == null) {
            return false;
        }
        LocalTime start = punchInWindowStart(shift);
        LocalTime end = punchInWindowEnd(shift);
        return !now.isBefore(start) && !now.isAfter(end);
    }

    /** Kiểm tra giờ hiện tại có trong cửa sổ chấm RA của ca không. */
    public static boolean isWithinPunchOutWindow(LocalTime now, ShiftCode shift) {
        if (now == null || shift == null) {
            return false;
        }
        LocalTime start = punchOutWindowStart(shift);
        LocalTime end = punchOutWindowEnd(shift);
        return !now.isBefore(start) && !now.isAfter(end);
    }

    /** Lý do chặn chấm VÀO ca (tiếng Việt) hoặc null nếu được phép. */
    public static String punchInBlockReason(LocalTime now, ShiftCode shift) {
        if (now == null) {
            return "Không xác định được giờ hệ thống.";
        }
        if (shift == null) {
            return "Không xác định được ca làm việc.";
        }
        LocalTime start = punchInWindowStart(shift);
        LocalTime end = punchInWindowEnd(shift);
        if (now.isBefore(start)) {
            return "Chưa đến giờ chấm công vào ca " + shiftLabel(shift) + ". Mở từ " + formatTime(start) + ".";
        }
        if (now.isAfter(end)) {
            return "Đã quá giờ chấm công vào ca " + shiftLabel(shift) + " (đến " + formatTime(end) + ").";
        }
        return null;
    }

    /** Lý do chặn chấm RA ca (tiếng Việt) hoặc null nếu được phép. */
    public static String punchOutBlockReason(LocalTime now, ShiftCode shift) {
        if (now == null) {
            return "Không xác định được giờ hệ thống.";
        }
        if (shift == null) {
            return "Không xác định được ca làm việc.";
        }
        LocalTime start = punchOutWindowStart(shift);
        LocalTime end = punchOutWindowEnd(shift);
        if (now.isBefore(start)) {
            return "Chưa đến giờ chấm công ra ca " + shiftLabel(shift) + ". Mở từ " + formatTime(start) + ".";
        }
        if (now.isAfter(end)) {
            return "Đã quá giờ chấm công ra ca " + shiftLabel(shift) + " (đến " + formatTime(end) + "). Liên hệ quản lý.";
        }
        return null;
    }

    /** Trần an toàn toàn quán (legacy). */
    public static boolean isWithinGlobalPunchWindow(LocalTime now) {
        return now != null && !now.isBefore(PUNCH_OPEN) && !now.isAfter(PUNCH_CLOSE);
    }

    public static String globalPunchWindowBlockReason(LocalTime now) {
        if (now == null) {
            return "Không xác định được giờ hệ thống.";
        }
        if (now.isBefore(PUNCH_OPEN)) {
            return "Chấm công mở từ 06:30. Vui lòng quay lại trong khung cho phép.";
        }
        if (now.isAfter(PUNCH_CLOSE)) {
            return "Đã quá 22:30, không thể chấm công. Vui lòng liên hệ quản lý.";
        }
        return null;
    }

    /**
     * Giờ làm tính lương theo ca: clamp trong [shiftStart, shiftEnd].
     * Vào sớm tính từ đầu ca, ra muộn tính đến cuối ca; vào muộn / ra sớm tính thật (thiếu phút).
     */
    public static double computePaidWorkHours(LocalTime checkIn, LocalTime checkOut, ShiftCode shift) {
        if (checkIn == null || checkOut == null || shift == null) {
            return 0.0;
        }
        LocalTime payStart = checkIn.isBefore(shift.getStart()) ? shift.getStart() : checkIn;
        LocalTime payEnd = checkOut.isAfter(shift.getEnd()) ? shift.getEnd() : checkOut;
        if (!payEnd.isAfter(payStart)) {
            return 0.0;
        }
        long minutes = Duration.between(payStart, payEnd).toMinutes();
        double hours = minutes / 60.0;
        return Math.round(hours * 100.0) / 100.0;
    }

    /** Fallback: tính lương theo khung cả ngày 07:00–22:00 (dữ liệu cũ không có shiftCode). */
    public static double computePaidWorkHours(LocalTime checkIn, LocalTime checkOut) {
        if (checkIn == null || checkOut == null) {
            return 0.0;
        }
        LocalTime payStart = checkIn.isBefore(PAY_DAY_START) ? PAY_DAY_START : checkIn;
        LocalTime payEnd = checkOut.isAfter(PAY_DAY_END) ? PAY_DAY_END : checkOut;
        if (!payEnd.isAfter(payStart)) {
            return 0.0;
        }
        long minutes = Duration.between(payStart, payEnd).toMinutes();
        double hours = minutes / 60.0;
        return Math.round(hours * 100.0) / 100.0;
    }

    private static String shiftLabel(ShiftCode shift) {
        return switch (shift) {
            case MORNING -> "sáng (07:00–14:00)";
            case AFTERNOON -> "chiều (14:00–18:00)";
            case EVENING -> "tối (18:00–22:00)";
            case FULL_DAY -> "cả ngày (07:00–22:00)";
        };
    }

    private static String formatTime(LocalTime t) {
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    // ========== GỘP CA LIỀN KỀ ==========

    /**
     * Đại diện cho một khung giờ làm việc (có thể là 1 ca đơn hoặc nhiều ca liền kề gộp lại).
     */
    public static class WorkBlock {
        private final LocalTime start;
        private final LocalTime end;
        private final List<ShiftCode> shifts;

        public WorkBlock(LocalTime start, LocalTime end, List<ShiftCode> shifts) {
            this.start = start;
            this.end = end;
            this.shifts = shifts;
        }

        public LocalTime getStart() { return start; }
        public LocalTime getEnd() { return end; }
        public List<ShiftCode> getShifts() { return shifts; }

        public boolean isMerged() { return shifts.size() > 1; }

        public String getLabel() {
            if (shifts.size() == 1) {
                return shiftLabel(shifts.get(0));
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < shifts.size(); i++) {
                if (i > 0) sb.append(" + ");
                sb.append(shiftLabelShort(shifts.get(i)));
            }
            sb.append(" (").append(formatTime(start)).append("–").append(formatTime(end)).append(")");
            return sb.toString();
        }

        private static String shiftLabelShort(ShiftCode sc) {
            return switch (sc) {
                case MORNING -> "sáng";
                case AFTERNOON -> "chiều";
                case EVENING -> "tối";
                case FULL_DAY -> "cả ngày";
            };
        }
    }

    /**
     * Gộp các ca liền kề thành các WorkBlock.
     * - Ca liền kề: ca trước kết thúc = ca sau bắt đầu (MORNING→AFTERNOON, AFTERNOON→EVENING).
     * - Ca rời (MORNING + EVENING): thành 2 WorkBlock riêng.
     * - FULL_DAY: luôn là 1 WorkBlock riêng.
     */
    public static List<WorkBlock> mergeAdjacentShifts(List<ShiftCode> shifts) {
        if (shifts == null || shifts.isEmpty()) {
            return List.of();
        }

        // FULL_DAY đứng riêng, không gộp với gì
        if (shifts.contains(ShiftCode.FULL_DAY)) {
            return List.of(new WorkBlock(ShiftCode.FULL_DAY.getStart(), ShiftCode.FULL_DAY.getEnd(), List.of(ShiftCode.FULL_DAY)));
        }

        // Sắp xếp theo giờ bắt đầu
        List<ShiftCode> sorted = new ArrayList<>(shifts);
        sorted.sort(Comparator.comparing(ShiftCode::getStart));

        List<WorkBlock> blocks = new ArrayList<>();
        List<ShiftCode> currentGroup = new ArrayList<>();
        LocalTime currentStart = null;
        LocalTime currentEnd = null;

        for (ShiftCode sc : sorted) {
            if (currentGroup.isEmpty()) {
                currentGroup.add(sc);
                currentStart = sc.getStart();
                currentEnd = sc.getEnd();
            } else if (sc.getStart().equals(currentEnd)) {
                // Liền kề: ca trước kết thúc = ca này bắt đầu
                currentGroup.add(sc);
                currentEnd = sc.getEnd();
            } else {
                // Rời: lưu group cũ, bắt đầu group mới
                blocks.add(new WorkBlock(currentStart, currentEnd, new ArrayList<>(currentGroup)));
                currentGroup.clear();
                currentGroup.add(sc);
                currentStart = sc.getStart();
                currentEnd = sc.getEnd();
            }
        }

        if (!currentGroup.isEmpty()) {
            blocks.add(new WorkBlock(currentStart, currentEnd, new ArrayList<>(currentGroup)));
        }

        return blocks;
    }

    /** Cửa sổ chấm VÀO cho WorkBlock. */
    public static LocalTime punchInWindowStart(WorkBlock block) {
        return block.getStart().minusMinutes(PUNCH_BUFFER_MINUTES);
    }

    public static LocalTime punchInWindowEnd(WorkBlock block) {
        return block.getEnd().minusMinutes(PUNCH_BUFFER_MINUTES);
    }

    /** Cửa sổ chấm RA cho WorkBlock. */
    public static LocalTime punchOutWindowStart(WorkBlock block) {
        return block.getStart().plusMinutes(PUNCH_BUFFER_MINUTES);
    }

    public static LocalTime punchOutWindowEnd(WorkBlock block) {
        return block.getEnd().plusMinutes(PUNCH_BUFFER_MINUTES);
    }

    public static boolean isWithinPunchInWindow(LocalTime now, WorkBlock block) {
        if (now == null || block == null) return false;
        LocalTime start = punchInWindowStart(block);
        LocalTime end = punchInWindowEnd(block);
        return !now.isBefore(start) && !now.isAfter(end);
    }

    public static boolean isWithinPunchOutWindow(LocalTime now, WorkBlock block) {
        if (now == null || block == null) return false;
        LocalTime start = punchOutWindowStart(block);
        LocalTime end = punchOutWindowEnd(block);
        return !now.isBefore(start) && !now.isAfter(end);
    }

    public static String punchInBlockReason(LocalTime now, WorkBlock block) {
        if (now == null) return "Không xác định được giờ hệ thống.";
        if (block == null) return "Không xác định được ca làm việc.";
        LocalTime start = punchInWindowStart(block);
        LocalTime end = punchInWindowEnd(block);
        if (now.isBefore(start)) {
            return "Chưa đến giờ chấm công vào " + block.getLabel() + ". Mở từ " + formatTime(start) + ".";
        }
        if (now.isAfter(end)) {
            return "Đã quá giờ chấm công vào " + block.getLabel() + " (đến " + formatTime(end) + ").";
        }
        return null;
    }

    public static String punchOutBlockReason(LocalTime now, WorkBlock block) {
        if (now == null) return "Không xác định được giờ hệ thống.";
        if (block == null) return "Không xác định được ca làm việc.";
        LocalTime start = punchOutWindowStart(block);
        LocalTime end = punchOutWindowEnd(block);
        if (now.isBefore(start)) {
            return "Chưa đến giờ chấm công ra " + block.getLabel() + ". Mở từ " + formatTime(start) + ".";
        }
        if (now.isAfter(end)) {
            return "Đã quá giờ chấm công ra " + block.getLabel() + " (đến " + formatTime(end) + "). Liên hệ quản lý.";
        }
        return null;
    }

    /** Giờ làm tính lương theo WorkBlock: clamp trong [block.start, block.end]. */
    public static double computePaidWorkHours(LocalTime checkIn, LocalTime checkOut, WorkBlock block) {
        if (checkIn == null || checkOut == null || block == null) return 0.0;
        LocalTime payStart = checkIn.isBefore(block.getStart()) ? block.getStart() : checkIn;
        LocalTime payEnd = checkOut.isAfter(block.getEnd()) ? block.getEnd() : checkOut;
        if (!payEnd.isAfter(payStart)) return 0.0;
        long minutes = Duration.between(payStart, payEnd).toMinutes();
        return Math.round(minutes / 60.0 * 100.0) / 100.0;
    }
}

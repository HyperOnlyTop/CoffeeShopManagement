package com.example.QuanLyQuanCafe.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.QuanLyQuanCafe.controller.dto.BookingReminderDto;
import com.example.QuanLyQuanCafe.model.BookingReminderKind;
import com.example.QuanLyQuanCafe.model.BookingStaffReminder;
import com.example.QuanLyQuanCafe.model.BookingStatus;
import com.example.QuanLyQuanCafe.model.TableBooking;
import com.example.QuanLyQuanCafe.repository.BookingStaffReminderRepository;
import com.example.QuanLyQuanCafe.repository.TableBookingRepository;

@Service
public class BookingStaffReminderService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TableBookingRepository tableBookingRepository;
    private final BookingStaffReminderRepository reminderRepository;

    public BookingStaffReminderService(
            TableBookingRepository tableBookingRepository,
            BookingStaffReminderRepository reminderRepository) {
        this.tableBookingRepository = tableBookingRepository;
        this.reminderRepository = reminderRepository;
    }

    /**
     * Tạo bản ghi nhắc (mỗi đặt bàn tối đa một lần cho loại FIFTEEN_MIN_BEFORE) khi đã trong cửa sổ 15 phút trước giờ hẹn.
     */
    @Transactional
    public int createDueFifteenMinuteReminders() {
        var now = java.time.LocalDateTime.now();
        var until = now.plusMinutes(15);
        List<TableBooking> due = tableBookingRepository.findByStatusAndBookingTimeGreaterThanAndBookingTimeLessThanEqual(
                BookingStatus.CONFIRMED, now, until);
        int added = 0;
        for (TableBooking b : due) {
            if (b == null || b.getId() == null) {
                continue;
            }
            if (reminderRepository.existsByTableBooking_IdAndKind(b.getId(), BookingReminderKind.FIFTEEN_MIN_BEFORE)) {
                continue;
            }
            try {
                BookingStaffReminder r = new BookingStaffReminder();
                r.setTableBooking(b);
                r.setKind(BookingReminderKind.FIFTEEN_MIN_BEFORE);
                r.setSummary(buildFifteenMinSummary(b));
                reminderRepository.save(r);
                added++;
            } catch (org.springframework.dao.DataIntegrityViolationException ignored) {
                // trùng lúc hai luồng — bỏ qua
            }
        }
        return added;
    }

    private static String buildFifteenMinSummary(TableBooking b) {
        String timeStr = b.getBookingTime() != null ? b.getBookingTime().format(FMT) : "?";
        String tablePart = b.getReservedTableNumber() != null
                ? " · Bàn " + b.getReservedTableNumber()
                : " · Chưa gán bàn";
        return "Trước 15 phút tới giờ: " + (b.getName() != null ? b.getName() : "Khách")
                + " — " + timeStr + " — " + b.getGuests() + " khách" + tablePart;
    }

    @Transactional(readOnly = true)
    public List<BookingReminderDto> listRecentForStaff() {
        return reminderRepository.findTop80ByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    private BookingReminderDto toDto(BookingStaffReminder r) {
        TableBooking b = r.getTableBooking();
        return new BookingReminderDto(
                r.getId(),
                b.getId(),
                r.getKind().name(),
                r.getSummary(),
                r.getCreatedAt(),
                r.getReadAt() != null,
                b.getName(),
                b.getBookingTime(),
                b.getReservedTableNumber()
        );
    }

    @Transactional(readOnly = true)
    public long countUnread() {
        return reminderRepository.countByReadAtIsNull();
    }

    @Transactional
    public boolean markRead(long reminderId) {
        Optional<BookingStaffReminder> opt = reminderRepository.findById(reminderId);
        if (opt.isEmpty()) {
            return false;
        }
        BookingStaffReminder r = opt.get();
        if (r.getReadAt() == null) {
            r.setReadAt(java.time.LocalDateTime.now());
            reminderRepository.save(r);
        }
        return true;
    }

    @Transactional
    public int markAllRead() {
        return reminderRepository.markAllUnreadAsRead(java.time.LocalDateTime.now());
    }

    @Transactional
    public Optional<String> applyStaffBookingAction(long bookingId, String action, String cancelNote) {
        Optional<TableBooking> opt = tableBookingRepository.findById(bookingId);
        if (opt.isEmpty()) {
            return Optional.of("Không tìm thấy đặt bàn.");
        }
        TableBooking b = opt.get();
        if (b.getBookingTime() != null && b.getBookingTime().toLocalDate().isBefore(LocalDate.now())) {
            return Optional.of("Đặt bàn đã qua ngày, không thể thao tác.");
        }
        BookingStatus st = b.getStatus();
        String a = action != null ? action.trim().toUpperCase() : "";
        switch (a) {
            case "CHECK_IN" -> {
                if (st != BookingStatus.CONFIRMED) {
                    return Optional.of("Chỉ xác nhận đến khi đặt đang ở trạng thái đã xác nhận.");
                }
                b.setStatus(BookingStatus.CHECKED_IN);
            }
            case "CANCEL" -> {
                if (st != BookingStatus.CONFIRMED) {
                    return Optional.of("Chỉ hủy được khi đặt đang chờ (đã xác nhận).");
                }
                b.setStatus(BookingStatus.CANCELLED);
                appendCancelReasonLine(b, cancelNote);
            }
            default -> {
                return Optional.of("Thao tác không hợp lệ.");
            }
        }
        tableBookingRepository.save(b);
        return Optional.empty();
    }

    private static void appendCancelReasonLine(TableBooking b, String cancelNote) {
        if (cancelNote == null || cancelNote.isBlank()) {
            return;
        }
        String line = "[Hủy] " + cancelNote.trim();
        String ex = b.getNote();
        if (ex == null || ex.isBlank()) {
            b.setNote(line);
        } else {
            b.setNote(ex.trim() + "\n" + line);
        }
    }
}

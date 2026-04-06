package com.example.QuanLyQuanCafe.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.QuanLyQuanCafe.config.BookingPolicy;
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
     * Tạo nhắc FIFTEEN_MIN_BEFORE: booking CONFIRMED có giờ đặt trong 15 phút tới.
     */
    @Transactional
    public int createFifteenMinBeforeReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusMinutes(BookingPolicy.REMINDER_BEFORE_MINUTES);
        List<TableBooking> due = tableBookingRepository.findByStatusAndBookingTimeGreaterThanAndBookingTimeLessThanEqual(
                BookingStatus.CONFIRMED, now, until);
        return createRemindersForList(due, BookingReminderKind.FIFTEEN_MIN_BEFORE,
                b -> "⏰ Còn 15 phút: " + formatBookingSummary(b));
    }

    /**
     * Tạo nhắc AT_BOOKING_TIME: booking CONFIRMED đã đến giờ đặt (trong khoảng 0-1 phút sau giờ đặt).
     */
    @Transactional
    public int createAtBookingTimeReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusMinutes(1);
        List<TableBooking> due = tableBookingRepository.findByStatusAndBookingTimeGreaterThanAndBookingTimeLessThanEqual(
                BookingStatus.CONFIRMED, from, now);
        return createRemindersForList(due, BookingReminderKind.AT_BOOKING_TIME,
                b -> "🔔 Đã đến giờ: " + formatBookingSummary(b));
    }

    /**
     * Tạo nhắc OVERDUE_15MIN: booking CONFIRMED đã quá 15 phút sau giờ đặt mà chưa tick đã đến hoặc hủy.
     */
    @Transactional
    public int createOverdueReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime overdueFrom = now.minusMinutes(BookingPolicy.REMINDER_OVERDUE_MINUTES + 1);
        LocalDateTime overdueTo = now.minusMinutes(BookingPolicy.REMINDER_OVERDUE_MINUTES);
        List<TableBooking> due = tableBookingRepository.findByStatusAndBookingTimeGreaterThanAndBookingTimeLessThanEqual(
                BookingStatus.CONFIRMED, overdueFrom, overdueTo);
        return createRemindersForList(due, BookingReminderKind.OVERDUE_15MIN,
                b -> "⚠️ Quá 15 phút chưa đến: " + formatBookingSummary(b));
    }

    private int createRemindersForList(List<TableBooking> bookings, BookingReminderKind kind,
                                       java.util.function.Function<TableBooking, String> summaryBuilder) {
        int added = 0;
        for (TableBooking b : bookings) {
            if (b == null || b.getId() == null) continue;
            if (reminderRepository.existsByTableBooking_IdAndKind(b.getId(), kind)) continue;
            try {
                BookingStaffReminder r = new BookingStaffReminder();
                r.setTableBooking(b);
                r.setKind(kind);
                r.setSummary(summaryBuilder.apply(b));
                reminderRepository.save(r);
                added++;
            } catch (org.springframework.dao.DataIntegrityViolationException ignored) {
            }
        }
        return added;
    }

    private static String formatBookingSummary(TableBooking b) {
        String timeStr = b.getBookingTime() != null ? b.getBookingTime().format(FMT) : "?";
        String tablePart = b.getReservedTableNumber() != null
                ? " · Bàn " + b.getReservedTableNumber()
                : " · Chưa gán bàn";
        return (b.getName() != null ? b.getName() : "Khách")
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

package com.example.QuanLyQuanCafe.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.QuanLyQuanCafe.service.BookingStaffReminderService;

@Component
public class BookingReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingReminderScheduler.class);

    private final BookingStaffReminderService bookingStaffReminderService;

    public BookingReminderScheduler(BookingStaffReminderService bookingStaffReminderService) {
        this.bookingStaffReminderService = bookingStaffReminderService;
    }

    /**
     * Mỗi phút quét và tạo 3 loại thông báo (chỉ cho đặt bàn {@code CONFIRMED}; lịch seed lịch sử
     * {@code COMPLETED}/{@code CANCELLED} trong {@link com.example.QuanLyQuanCafe.config.BookingDataSeeder}
     * không kích hoạt luồng này).
     * 1. FIFTEEN_MIN_BEFORE - 15 phút trước giờ đặt
     * 2. AT_BOOKING_TIME - đúng giờ đặt
     * 3. OVERDUE_15MIN - 15 phút sau giờ đặt nếu chưa xử lý
     */
    @Scheduled(fixedRate = 60_000)
    public void tickBookingReminders() {
        try {
            int before = bookingStaffReminderService.createFifteenMinBeforeReminders();
            int atTime = bookingStaffReminderService.createAtBookingTimeReminders();
            int overdue = bookingStaffReminderService.createOverdueReminders();

            int total = before + atTime + overdue;
            if (total > 0) {
                log.info("Booking reminders created: {} before, {} at-time, {} overdue.", before, atTime, overdue);
            }
        } catch (Exception ex) {
            log.warn("Booking reminder scheduler failed: {}", ex.getMessage());
        }
    }
}

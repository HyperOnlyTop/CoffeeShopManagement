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

    /** Mỗi phút quét đặt CONFIRMED sắp tới trong 15 phút và ghi nhắc (idempotent theo đặt bàn). */
    @Scheduled(fixedRate = 60_000)
    public void tickFifteenMinuteReminders() {
        try {
            int n = bookingStaffReminderService.createDueFifteenMinuteReminders();
            if (n > 0) {
                log.info("Booking reminders: created {} fifteen-minute notice(s).", n);
            }
        } catch (Exception ex) {
            log.warn("Booking reminder scheduler failed: {}", ex.getMessage());
        }
    }
}

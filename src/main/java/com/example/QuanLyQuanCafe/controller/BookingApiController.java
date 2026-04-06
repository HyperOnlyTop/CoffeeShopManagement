package com.example.QuanLyQuanCafe.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.controller.dto.BookingReminderDto;
import com.example.QuanLyQuanCafe.model.TableBooking;
import com.example.QuanLyQuanCafe.repository.TableBookingRepository;
import com.example.QuanLyQuanCafe.service.BookingStaffReminderService;

@RestController
@RequestMapping("/api/bookings")
public class BookingApiController {

    private final TableBookingRepository tableBookingRepository;
    private final BookingStaffReminderService bookingStaffReminderService;

    public BookingApiController(
            TableBookingRepository tableBookingRepository,
            BookingStaffReminderService bookingStaffReminderService) {
        this.tableBookingRepository = tableBookingRepository;
        this.bookingStaffReminderService = bookingStaffReminderService;
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isBookingStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(a -> {
            String g = a.getAuthority();
            return "ROLE_ADMIN".equals(g) || "ROLE_CASHIER".equals(g) || "ROLE_SERVER".equals(g);
        });
    }

    @GetMapping("/by-phone")
    public ResponseEntity<List<TableBooking>> getByPhone(@RequestParam("phone") String phone) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(tableBookingRepository.findByPhoneOrderByCreatedAtDesc(phone.trim()));
    }

    @GetMapping("/reminders")
    public ResponseEntity<Map<String, Object>> listReminders() {
        if (!isBookingStaff()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<BookingReminderDto> list = bookingStaffReminderService.listRecentForStaff();
        long unread = bookingStaffReminderService.countUnread();
        return ResponseEntity.ok(Map.of(
                "reminders", list,
                "unreadCount", unread
        ));
    }

    @PostMapping("/reminders/{id}/read")
    public ResponseEntity<Map<String, Object>> markReminderRead(@PathVariable("id") Long id) {
        if (!isBookingStaff()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }
        boolean ok = bookingStaffReminderService.markRead(id);
        if (!ok) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "unreadCount", bookingStaffReminderService.countUnread()
        ));
    }

    @PostMapping("/reminders/read-all")
    public ResponseEntity<Map<String, Object>> markAllRemindersRead() {
        if (!isBookingStaff()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        int n = bookingStaffReminderService.markAllRead();
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "marked", n,
                "unreadCount", bookingStaffReminderService.countUnread()
        ));
    }

    @PostMapping("/{bookingId}/staff-action")
    public ResponseEntity<Map<String, Object>> staffBookingAction(
            @PathVariable("bookingId") Long bookingId,
            @RequestParam("action") String action,
            @RequestParam(value = "note", required = false) String note) {
        if (!isBookingStaff()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (bookingId == null) {
            return ResponseEntity.badRequest().build();
        }
        var err = bookingStaffReminderService.applyStaffBookingAction(bookingId, action, note);
        if (err.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", err.get()));
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }
}

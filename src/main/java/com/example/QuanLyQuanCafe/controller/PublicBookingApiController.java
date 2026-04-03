package com.example.QuanLyQuanCafe.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.config.BookingPolicy;
import com.example.QuanLyQuanCafe.controller.dto.BookingLookupDto;
import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.BookingStatus;
import com.example.QuanLyQuanCafe.model.TableBooking;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.repository.TableBookingRepository;

@RestController
@RequestMapping("/api/public/bookings")
public class PublicBookingApiController {

    private static final DateTimeFormatter BOOKING_TIME_DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TableBookingRepository tableBookingRepository;
    private final AppUserRepository appUserRepository;

    public PublicBookingApiController(TableBookingRepository tableBookingRepository,
                                      AppUserRepository appUserRepository) {
        this.tableBookingRepository = tableBookingRepository;
        this.appUserRepository = appUserRepository;
    }

    private static BookingLookupDto toDto(TableBooking b) {
        String note = b.getNote();
        String timeStr = b.getBookingTime() != null
                ? b.getBookingTime().format(BOOKING_TIME_DISPLAY)
                : "";
        return new BookingLookupDto(
                b.getId(),
                b.getName(),
                timeStr,
                b.getGuests(),
                b.getStatus() != null ? b.getStatus().name() : "",
                note != null ? note : ""
        );
    }

    /**
     * Khách đã đăng nhập: danh sách đặt bàn theo SĐT và/hoặc email trong tài khoản.
     */
    @GetMapping("/me")
    public ResponseEntity<List<BookingLookupDto>> myBookings(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        AppUser user = appUserRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }

        Set<Long> seen = new HashSet<>();
        List<TableBooking> merged = new ArrayList<>();
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            for (TableBooking b : tableBookingRepository.findByPhoneOrderByCreatedAtDesc(user.getPhone().trim())) {
                if (seen.add(b.getId())) {
                    merged.add(b);
                }
            }
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            for (TableBooking b : tableBookingRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(user.getEmail().trim())) {
                if (seen.add(b.getId())) {
                    merged.add(b);
                }
            }
        }

        merged.sort(Comparator.comparing(TableBooking::getCreatedAt).reversed());
        return ResponseEntity.ok(merged.stream().map(PublicBookingApiController::toDto).toList());
    }

    /**
     * Khách chưa đăng nhập: tra cứu theo đúng một trong hai — SĐT hoặc email đã dùng lúc đặt.
     */
    @GetMapping("/lookup")
    public ResponseEntity<?> lookup(@RequestParam(value = "phone", required = false) String phone,
                                    @RequestParam(value = "email", required = false) String email) {
        boolean hasPhone = phone != null && !phone.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        if (hasPhone == hasEmail) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Vui lòng nhập SĐT hoặc email (chỉ một trong hai)."));
        }

        List<TableBooking> list = hasPhone
                ? tableBookingRepository.findByPhoneOrderByCreatedAtDesc(phone.trim())
                : tableBookingRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(email.trim());

        return ResponseEntity.ok(list.stream().map(PublicBookingApiController::toDto).toList());
    }

    /**
     * Đặt bàn từ landing (AJAX), không redirect.
     */
    @PostMapping
    public ResponseEntity<?> createBooking(@RequestParam("name") String name,
                                         @RequestParam("phone") String phone,
                                         @RequestParam(value = "email", required = false) String email,
                                         @RequestParam("date") String dateStr,
                                         @RequestParam("time") String timeStr,
                                         @RequestParam("guests") int guests,
                                         @RequestParam(value = "note", required = false) String note) {
        try {
            if (name == null || name.isBlank() || phone == null || phone.isBlank() || guests < 1) {
                return ResponseEntity.badRequest()
                        .body(Map.of("ok", false, "code", "invalidTime",
                                "message", "Vui lòng nhập đủ họ tên, số điện thoại và số khách."));
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate date = LocalDate.parse(dateStr, dateFormatter);
            LocalDate today = LocalDate.now();
            if (date.isBefore(today)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("ok", false, "code", "invalidTime",
                                "message", "Không thể chọn ngày trong quá khứ."));
            }

            LocalTime time = LocalTime.parse(timeStr);
            LocalDateTime bookingTime = LocalDateTime.of(date, time);
            LocalDateTime now = LocalDateTime.now();

            if (bookingTime.isBefore(now)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("ok", false, "code", "invalidTime",
                                "message", "Vui lòng chọn ngày và giờ trong tương lai."));
            }

            if (bookingTime.isBefore(now.plusHours(BookingPolicy.MIN_LEAD_HOURS))) {
                return ResponseEntity.badRequest()
                        .body(Map.of("ok", false, "code", "tooSoon",
                                "message", "Giờ đến phải cách hiện tại ít nhất "
                                        + BookingPolicy.MIN_LEAD_HOURS + " giờ."));
            }

            if (tableBookingRepository.existsByBookingTime(bookingTime)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("ok", false, "code", "conflict",
                                "message", "Khung giờ này đã có khách đặt. Vui lòng chọn thời gian khác."));
            }

            TableBooking booking = new TableBooking();
            booking.setName(name.trim());
            booking.setPhone(phone.trim());
            booking.setEmail(email != null ? email.trim() : null);
            booking.setBookingTime(bookingTime);
            booking.setGuests(guests);
            booking.setNote(note != null ? note.trim() : null);
            booking.setStatus(BookingStatus.CONFIRMED);
            tableBookingRepository.save(booking);

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "message", "Đặt bàn thành công. Quán sẽ giữ bàn cho bạn tối đa 15 phút sau giờ đã đặt."));
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "code", "invalidTime",
                            "message", "Thời gian đặt bàn không hợp lệ."));
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "code", "error",
                            "message", "Có lỗi khi đặt bàn. Vui lòng thử lại."));
        }
    }
}

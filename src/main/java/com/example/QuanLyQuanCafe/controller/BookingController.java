package com.example.QuanLyQuanCafe.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.QuanLyQuanCafe.config.BookingPolicy;
import com.example.QuanLyQuanCafe.model.BookingStatus;
import com.example.QuanLyQuanCafe.model.TableBooking;
import com.example.QuanLyQuanCafe.repository.TableBookingRepository;
import com.example.QuanLyQuanCafe.service.BookingTableValidationService;

@Controller
public class BookingController {

    private final TableBookingRepository tableBookingRepository;
    private final BookingTableValidationService bookingTableValidationService;

    public BookingController(TableBookingRepository tableBookingRepository,
                             BookingTableValidationService bookingTableValidationService) {
        this.tableBookingRepository = tableBookingRepository;
        this.bookingTableValidationService = bookingTableValidationService;
    }

    @PostMapping("/booking")
    public String createBooking(@RequestParam("name") String name,
                                @RequestParam("phone") String phone,
                                @RequestParam(value = "email", required = false) String email,
                                @RequestParam("date") String dateStr,
                                @RequestParam("time") String timeStr,
                                @RequestParam("guests") int guests,
                                @RequestParam(value = "note", required = false) String note) {

        try {
            if (name == null || name.isBlank() || phone == null || phone.isBlank() || guests < 1) {
                return "redirect:/?bookingError=invalidTime#booking";
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate date = LocalDate.parse(dateStr, dateFormatter);

            // Không cho đặt ngày trong quá khứ
            LocalDate today = LocalDate.now();
            if (date.isBefore(today)) {
                return "redirect:/?bookingError=invalidTime#booking";
            }

            // timeStr có thể là "Chọn giờ" -> parse sẽ lỗi
            LocalTime time = LocalTime.parse(timeStr);
            LocalDateTime bookingTime = LocalDateTime.of(date, time);

            LocalDateTime now = LocalDateTime.now();

            // Nếu đặt trong quá khứ (so với hiện tại) thì báo lỗi
            if (bookingTime.isBefore(now)) {
                return "redirect:/?bookingError=invalidTime#booking";
            }

            // Giờ đến phải cách hiện tại ít nhất MIN_LEAD_HOURS (đồng bộ với form trên landing)
            if (bookingTime.isBefore(now.plusHours(BookingPolicy.MIN_LEAD_HOURS))) {
                return "redirect:/?bookingError=tooSoon#booking";
            }

            // Nếu đã có đặt bàn trùng chính xác khung giờ này thì báo lỗi
            if (tableBookingRepository.existsByBookingTime(bookingTime)) {
                return "redirect:/?bookingError=conflict#booking";
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

            return "redirect:/?bookingSuccess=1#booking";
        } catch (DateTimeParseException ex) {
            return "redirect:/?bookingError=invalidTime#booking";
        } catch (Exception ex) {
            return "redirect:/?bookingError=1#booking";
        }
    }

    @GetMapping("/Booking/new")
    public String newBookingForm(Model model) {
        TableBooking booking = new TableBooking();
        booking.setGuests(2);
        model.addAttribute("booking", booking);
        return "admin/BookingForm";
    }

    @GetMapping("/Booking/edit/{id}")
    public String editBookingForm(@PathVariable("id") Long id, Model model) {
        Optional<TableBooking> optional = tableBookingRepository.findById(id);
        if (optional.isEmpty()) {
            return "redirect:/Booking";
        }
        TableBooking b = optional.get();
        if (b.getBookingTime() != null && b.getBookingTime().toLocalDate().isBefore(LocalDate.now())) {
            return "redirect:/Booking?pastBooking=1";
        }
        model.addAttribute("booking", b);
        return "admin/BookingForm";
    }

    @PostMapping("/Booking/save")
    public String saveBookingFromAdmin(TableBooking booking, Model model) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        if (booking.getBookingTime() != null && booking.getBookingTime().toLocalDate().isBefore(today)) {
            model.addAttribute("booking", booking);
            model.addAttribute("errorMessage", "Không dùng ngày đặt trong quá khứ.");
            return "admin/BookingForm";
        }

        // Với booking mới: phải đặt ít nhất 20 phút trong tương lai
        if (booking.getId() == null && booking.getBookingTime() != null) {
            LocalDateTime minTime = now.plusMinutes(BookingPolicy.MIN_LEAD_MINUTES_STAFF);
            if (booking.getBookingTime().isBefore(minTime)) {
                model.addAttribute("booking", booking);
                model.addAttribute("errorMessage", "Thời gian đặt bàn phải ít nhất " + BookingPolicy.MIN_LEAD_MINUTES_STAFF + " phút trong tương lai.");
                return "admin/BookingForm";
            }
        }

        if (booking.getId() != null) {
            Optional<TableBooking> existing = tableBookingRepository.findById(booking.getId());
            if (existing.isEmpty()) {
                return "redirect:/Booking";
            }
            TableBooking old = existing.get();
            if (old.getBookingTime() != null && old.getBookingTime().toLocalDate().isBefore(today)) {
                model.addAttribute("booking", booking);
                model.addAttribute("errorMessage", "Đặt bàn đã qua ngày, không được sửa.");
                return "admin/BookingForm";
            }
            // Nếu sửa thời gian, cũng phải đảm bảo ít nhất 20 phút trong tương lai
            if (booking.getBookingTime() != null && !booking.getBookingTime().equals(old.getBookingTime())) {
                LocalDateTime minTime = now.plusMinutes(BookingPolicy.MIN_LEAD_MINUTES_STAFF);
                if (booking.getBookingTime().isBefore(minTime)) {
                    model.addAttribute("booking", booking);
                    model.addAttribute("errorMessage", "Thời gian đặt bàn phải ít nhất " + BookingPolicy.MIN_LEAD_MINUTES_STAFF + " phút trong tương lai.");
                    return "admin/BookingForm";
                }
            }
            booking.setCreatedAt(old.getCreatedAt());
            if (booking.getStatus() == null) {
                booking.setStatus(old.getStatus());
            }
        }

        if (booking.getBookingTime() != null) {
            boolean conflict = booking.getId() == null
                    ? tableBookingRepository.existsByBookingTime(booking.getBookingTime())
                    : tableBookingRepository.existsByBookingTimeAndIdNot(booking.getBookingTime(), booking.getId());
            if (conflict) {
                model.addAttribute("booking", booking);
                model.addAttribute("errorMessage", "Đã có đặt bàn trùng khung giờ này.");
                return "admin/BookingForm";
            }
        }

        Optional<String> tableErr = bookingTableValidationService.validateReservedTableForBooking(
                booking.getReservedTableNumber(), booking.getBookingTime(), booking.getId());
        if (tableErr.isPresent()) {
            model.addAttribute("booking", booking);
            model.addAttribute("errorMessage", tableErr.get());
            return "admin/BookingForm";
        }

        tableBookingRepository.save(booking);
        return "redirect:/Booking";
    }
}
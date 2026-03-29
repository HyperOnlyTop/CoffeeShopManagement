package com.example.QuanLyQuanCafe.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.QuanLyQuanCafe.model.TableBooking;
import com.example.QuanLyQuanCafe.repository.TableBookingRepository;

@Controller
public class BookingController {

    private final TableBookingRepository tableBookingRepository;

    public BookingController(TableBookingRepository tableBookingRepository) {
        this.tableBookingRepository = tableBookingRepository;
    }

    @PostMapping("/booking")
    public String createBooking(@RequestParam("name") String name,
                                @RequestParam("phone") String phone,
                                @RequestParam(value = "email", required = false) String email,
                                @RequestParam("date") String dateStr,
                                @RequestParam("time") String timeStr,
                                @RequestParam("guests") int guests,
                                @RequestParam(value = "note", required = false) String note) {

        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(dateStr, dateFormatter);

        // Nếu ngày đặt sau ngày hiện tại thì coi là không hợp lệ
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            return "redirect:/?bookingError=invalidTime#booking";
        }

        LocalTime time = LocalTime.parse(timeStr);
        LocalDateTime bookingTime = LocalDateTime.of(date, time);

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

        tableBookingRepository.save(booking);

        return "redirect:/?bookingSuccess=1#booking";
    }

    @GetMapping("/Booking/new")
    public String newBookingForm(Model model) {
        TableBooking booking = new TableBooking();
        booking.setGuests(2);
        model.addAttribute("booking", booking);
        return "BookingForm";
    }

    @GetMapping("/Booking/edit/{id}")
    public String editBookingForm(@PathVariable("id") Long id, Model model) {
        Optional<TableBooking> optional = tableBookingRepository.findById(id);
        if (optional.isEmpty()) {
            return "redirect:/Booking";
        }
        model.addAttribute("booking", optional.get());
        return "BookingForm";
    }

    @PostMapping("/Booking/save")
    public String saveBookingFromAdmin(TableBooking booking) {
        
        
        if (booking.getId() != null) {
            // lấy bản ghi cũ để giữ createdAt
            Optional<TableBooking> existing = tableBookingRepository.findById(booking.getId());
            existing.ifPresent(old -> booking.setCreatedAt(old.getCreatedAt()));
        }
        
        if (booking.getBookingTime() != null) {
            boolean existsSameTime = tableBookingRepository.existsByBookingTime(booking.getBookingTime());
            if (existsSameTime && booking.getId() == null) {
                return "redirect:/Booking?error=conflict";
            }
        }
        tableBookingRepository.save(booking);
        return "redirect:/Booking";
    }

    @GetMapping("/Booking/delete/{id}")
    public String deleteBooking(@PathVariable("id") Long id) {
        if (id != null) {
            tableBookingRepository.deleteById(id);
        }
        return "redirect:/Booking";
    }
}

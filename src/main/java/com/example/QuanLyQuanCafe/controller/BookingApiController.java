package com.example.QuanLyQuanCafe.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.model.TableBooking;
import com.example.QuanLyQuanCafe.repository.TableBookingRepository;

@RestController
@RequestMapping("/api/bookings")
public class BookingApiController {

    private final TableBookingRepository tableBookingRepository;

    public BookingApiController(TableBookingRepository tableBookingRepository) {
        this.tableBookingRepository = tableBookingRepository;
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
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
}


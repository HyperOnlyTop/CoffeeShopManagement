package com.example.QuanLyQuanCafe.controller.dto;

public record BookingLookupDto(
        long id,
        String customerName,
        String bookingTime,
        int guests,
        String status,
        String note
) {}

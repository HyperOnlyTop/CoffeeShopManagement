package com.example.QuanLyQuanCafe.controller.dto;

import java.time.LocalDateTime;

public record BookingReminderDto(
        long id,
        long bookingId,
        String kind,
        String summary,
        LocalDateTime createdAt,
        boolean acknowledged,
        String customerName,
        LocalDateTime bookingTime,
        Integer reservedTableNumber
) {}

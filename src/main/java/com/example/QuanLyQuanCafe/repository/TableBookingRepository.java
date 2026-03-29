package com.example.QuanLyQuanCafe.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuanLyQuanCafe.model.TableBooking;

public interface TableBookingRepository extends JpaRepository<TableBooking, Long> {

	boolean existsByBookingTime(LocalDateTime bookingTime);
}

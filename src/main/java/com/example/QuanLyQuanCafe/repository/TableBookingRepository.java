package com.example.QuanLyQuanCafe.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuanLyQuanCafe.model.TableBooking;
import com.example.QuanLyQuanCafe.model.BookingStatus;

public interface TableBookingRepository extends JpaRepository<TableBooking, Long> {

	boolean existsByBookingTime(LocalDateTime bookingTime);

	boolean existsByPhoneAndBookingTime(String phone, LocalDateTime bookingTime);

    List<TableBooking> findByPhoneOrderByCreatedAtDesc(String phone);

    List<TableBooking> findByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    List<TableBooking> findByStatusAndBookingTimeBetween(BookingStatus status, LocalDateTime start, LocalDateTime end);

    List<TableBooking> findByReservedTableNumberAndStatus(Integer reservedTableNumber, BookingStatus status);

    boolean existsByBookingTimeAndIdNot(LocalDateTime bookingTime, Long id);
}
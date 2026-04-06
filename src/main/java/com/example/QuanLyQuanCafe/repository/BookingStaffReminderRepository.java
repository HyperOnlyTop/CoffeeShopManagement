package com.example.QuanLyQuanCafe.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.QuanLyQuanCafe.model.BookingReminderKind;
import com.example.QuanLyQuanCafe.model.BookingStaffReminder;

public interface BookingStaffReminderRepository extends JpaRepository<BookingStaffReminder, Long> {

    boolean existsByTableBooking_IdAndKind(Long tableBookingId, BookingReminderKind kind);

    List<BookingStaffReminder> findTop80ByOrderByCreatedAtDesc();

    long countByReadAtIsNull();

    @Modifying
    @Query("UPDATE BookingStaffReminder r SET r.readAt = :t WHERE r.readAt IS NULL")
    int markAllUnreadAsRead(@Param("t") LocalDateTime t);
}

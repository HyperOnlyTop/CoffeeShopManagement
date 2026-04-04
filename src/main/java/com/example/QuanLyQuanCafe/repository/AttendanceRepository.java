package com.example.QuanLyQuanCafe.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.QuanLyQuanCafe.model.Attendance;
import com.example.QuanLyQuanCafe.model.Staff;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStaffAndWorkDateBetween(Staff staff, LocalDate start, LocalDate end);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.staff s WHERE a.workDate >= :start AND a.workDate <= :end ORDER BY a.workDate DESC, s.name ASC")
    List<Attendance> findReportByWorkDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}

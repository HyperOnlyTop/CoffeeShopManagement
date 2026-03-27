package com.example.QuanLyQuanCafe.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuanLyQuanCafe.model.Attendance;
import com.example.QuanLyQuanCafe.model.Staff;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStaffAndWorkDateBetween(Staff staff, LocalDate start, LocalDate end);
}

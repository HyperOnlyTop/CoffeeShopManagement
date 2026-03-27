package com.example.QuanLyQuanCafe.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.QuanLyQuanCafe.model.Attendance;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.repository.AttendanceRepository;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public List<Attendance> findByStaffAndDateRange(Staff staff, LocalDate start, LocalDate end) {
        return attendanceRepository.findByStaffAndWorkDateBetween(staff, start, end);
    }
}

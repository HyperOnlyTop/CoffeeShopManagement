package com.example.QuanLyQuanCafe.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuanLyQuanCafe.model.ShiftCode;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.model.StaffShiftAssignment;

public interface StaffShiftAssignmentRepository extends JpaRepository<StaffShiftAssignment, Long> {
    List<StaffShiftAssignment> findByWorkDate(LocalDate workDate);
    boolean existsByStaffAndWorkDateAndShiftCode(Staff staff, LocalDate workDate, ShiftCode shiftCode);
    void deleteByStaffAndWorkDateAndShiftCode(Staff staff, LocalDate workDate, ShiftCode shiftCode);
}


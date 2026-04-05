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

    /** Lấy tất cả ca đã gán cho nhân viên trong một ngày (có thể nhiều ca). */
    List<StaffShiftAssignment> findByStaffAndWorkDate(Staff staff, LocalDate workDate);
}


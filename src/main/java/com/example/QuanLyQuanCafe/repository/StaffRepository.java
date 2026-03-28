package com.example.QuanLyQuanCafe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.model.StaffRole;
import com.example.QuanLyQuanCafe.model.StaffStatus;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    List<Staff> findByStatus(StaffStatus status);

    List<Staff> findByRole(StaffRole role);

    Staff findByName(String name);
}

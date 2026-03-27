package com.example.QuanLyQuanCafe.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.QuanLyQuanCafe.controller.dto.StaffCreateRequest;
import com.example.QuanLyQuanCafe.controller.dto.StaffUpdateRequest;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.model.StaffRole;
import com.example.QuanLyQuanCafe.model.StaffStatus;
import com.example.QuanLyQuanCafe.repository.StaffRepository;

@Service
public class StaffService {

    private final StaffRepository staffRepository;

    public StaffService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    public List<Staff> findAll() {
        return staffRepository.findAll();
    }

    public List<Staff> findByStatus(StaffStatus status) {
        return staffRepository.findByStatus(status);
    }

    public List<Staff> findByRole(StaffRole role) {
        return staffRepository.findByRole(role);
    }

    public Staff create(StaffCreateRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tên nhân viên là bắt buộc");
        }

        Staff staff = new Staff();
        staff.setName(request.getName());

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            staff.setPhone(request.getPhone());
        }

        if (request.getRole() != null) {
            try {
                staff.setRole(StaffRole.valueOf(request.getRole()));
            } catch (IllegalArgumentException ex) {
                // ignore invalid role
            }
        }

        if (request.getStatus() != null) {
            try {
                staff.setStatus(StaffStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException ex) {
                // ignore invalid status
            }
        } else {
            staff.setStatus(StaffStatus.ACTIVE);
        }

        if (request.getShift() != null) {
            staff.setShift(request.getShift());
        }

        if (request.getSalary() != null) {
            staff.setSalary(request.getSalary());
        }

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            staff.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getStartDate() != null) {
            staff.setStartDate(request.getStartDate());
        } else {
            staff.setStartDate(LocalDate.now());
        }

        return staffRepository.save(staff);
    }

    public Staff update(StaffUpdateRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("Staff id is required");
        }

        Staff staff = staffRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên"));

        if (request.getName() != null) {
            staff.setName(request.getName());
        }

        if (request.getPhone() != null) {
            staff.setPhone(request.getPhone());
        }

        if (request.getRole() != null) {
            try {
                staff.setRole(StaffRole.valueOf(request.getRole()));
            } catch (IllegalArgumentException ex) {
                // ignore invalid role
            }
        }

        if (request.getStatus() != null) {
            try {
                staff.setStatus(StaffStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException ex) {
                // ignore invalid status
            }
        }

        if (request.getShift() != null) {
            staff.setShift(request.getShift());
        }

        if (request.getSalary() != null) {
            staff.setSalary(request.getSalary());
        }

        if (request.getStartDate() != null) {
            staff.setStartDate(request.getStartDate());
        }

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            staff.setAvatarUrl(request.getAvatarUrl());
        }

        return staffRepository.save(staff);
    }
}

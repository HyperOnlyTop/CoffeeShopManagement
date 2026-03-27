package com.example.QuanLyQuanCafe.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.controller.dto.StaffCreateRequest;
import com.example.QuanLyQuanCafe.controller.dto.StaffUpdateRequest;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.model.StaffRole;
import com.example.QuanLyQuanCafe.model.StaffStatus;
import com.example.QuanLyQuanCafe.service.StaffService;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public List<Staff> getAll() {
        return staffService.findAll();
    }

    @GetMapping("/status/{status}")
    public List<Staff> getByStatus(@PathVariable("status") StaffStatus status) {
        return staffService.findByStatus(status);
    }

    @GetMapping("/role/{role}")
    public List<Staff> getByRole(@PathVariable("role") StaffRole role) {
        return staffService.findByRole(role);
    }

    @PostMapping
    public Staff create(@RequestBody StaffCreateRequest request) {
        return staffService.create(request);
    }

    @PostMapping("/update")
    public Staff update(@RequestBody StaffUpdateRequest request) {
        return staffService.update(request);
    }
}

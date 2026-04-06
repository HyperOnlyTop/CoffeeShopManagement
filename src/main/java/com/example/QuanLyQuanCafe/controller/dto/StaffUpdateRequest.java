package com.example.QuanLyQuanCafe.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StaffUpdateRequest {

    private Long id;
    private String name;
    private String role;
    private String status;
    private String shift;
    private BigDecimal salary;
    private LocalDate startDate;
    private String avatarUrl;
    private String phone;
    private LocalDate leaveFrom;
    private LocalDate leaveTo;
    private LocalDate leftOn;

    /** Cập nhật email tài khoản liên kết (nếu có). */
    private String linkedAccountEmail;
    /** Đổi mật khẩu đăng nhập; để trống = giữ nguyên. */
    private String linkedAccountNewPassword;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getLeaveFrom() {
        return leaveFrom;
    }

    public void setLeaveFrom(LocalDate leaveFrom) {
        this.leaveFrom = leaveFrom;
    }

    public LocalDate getLeaveTo() {
        return leaveTo;
    }

    public void setLeaveTo(LocalDate leaveTo) {
        this.leaveTo = leaveTo;
    }

    public LocalDate getLeftOn() {
        return leftOn;
    }

    public void setLeftOn(LocalDate leftOn) {
        this.leftOn = leftOn;
    }

    public String getLinkedAccountEmail() {
        return linkedAccountEmail;
    }

    public void setLinkedAccountEmail(String linkedAccountEmail) {
        this.linkedAccountEmail = linkedAccountEmail;
    }

    public String getLinkedAccountNewPassword() {
        return linkedAccountNewPassword;
    }

    public void setLinkedAccountNewPassword(String linkedAccountNewPassword) {
        this.linkedAccountNewPassword = linkedAccountNewPassword;
    }
}

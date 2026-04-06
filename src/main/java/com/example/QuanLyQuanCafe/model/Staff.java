package com.example.QuanLyQuanCafe.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "staff")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StaffRole role;

    /** Lương theo giờ (VNĐ/giờ). Lương tạm tính = tổng giờ chấm công trong kỳ × giá trị này. Không dùng cho chủ quán (tài khoản ADMIN không gán staff). */
    @Column(precision = 12, scale = 0)
    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StaffStatus status;

    @Column(length = 100)
    private String shift;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Nghỉ phép: ngày đầu (kể cả nghỉ 1 ngày thì có thể trùng {@link #leaveTo}). */
    @Column(name = "leave_from")
    private LocalDate leaveFrom;

    @Column(name = "leave_to")
    private LocalDate leaveTo;

    /** Đã nghỉ việc: ngày làm việc cuối / nghỉ việc. */
    @Column(name = "left_on")
    private LocalDate leftOn;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public StaffRole getRole() {
        return role;
    }

    public void setRole(StaffRole role) {
        this.role = role;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public StaffStatus getStatus() {
        return status;
    }

    public void setStatus(StaffStatus status) {
        this.status = status;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
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
}

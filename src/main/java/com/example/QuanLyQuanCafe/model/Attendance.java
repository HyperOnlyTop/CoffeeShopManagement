package com.example.QuanLyQuanCafe.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "work_date")
    private LocalDate workDate;

    @Column(name = "check_in")
    private LocalTime checkIn;

    @Column(name = "check_out")
    private LocalTime checkOut;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AttendanceStatus status;

    @Column(name = "work_hours")
    private Double workHours;

    /** Ca làm việc tương ứng với segment chấm công này (null nếu dữ liệu cũ chưa gắn ca hoặc ca gộp). */
    @Enumerated(EnumType.STRING)
    @Column(name = "shift_code", length = 20)
    private ShiftCode shiftCode;

    /** Giờ bắt đầu khung làm việc (dùng cho ca gộp hoặc ca đơn). */
    @Column(name = "block_start")
    private LocalTime blockStart;

    /** Giờ kết thúc khung làm việc (dùng cho ca gộp hoặc ca đơn). */
    @Column(name = "block_end")
    private LocalTime blockEnd;

    /** Danh sách mã ca trong khung gộp, phân cách bằng dấu phẩy (vd: "MORNING,AFTERNOON"). */
    @Column(name = "block_shifts", length = 100)
    private String blockShifts;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public LocalTime getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalTime checkIn) {
        this.checkIn = checkIn;
    }

    public LocalTime getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalTime checkOut) {
        this.checkOut = checkOut;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public Double getWorkHours() {
        return workHours;
    }

    public void setWorkHours(Double workHours) {
        this.workHours = workHours;
    }

    public ShiftCode getShiftCode() {
        return shiftCode;
    }

    public void setShiftCode(ShiftCode shiftCode) {
        this.shiftCode = shiftCode;
    }

    public LocalTime getBlockStart() {
        return blockStart;
    }

    public void setBlockStart(LocalTime blockStart) {
        this.blockStart = blockStart;
    }

    public LocalTime getBlockEnd() {
        return blockEnd;
    }

    public void setBlockEnd(LocalTime blockEnd) {
        this.blockEnd = blockEnd;
    }

    public String getBlockShifts() {
        return blockShifts;
    }

    public void setBlockShifts(String blockShifts) {
        this.blockShifts = blockShifts;
    }
}

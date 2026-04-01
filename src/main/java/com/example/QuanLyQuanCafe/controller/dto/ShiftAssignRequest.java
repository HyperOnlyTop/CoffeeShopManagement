package com.example.QuanLyQuanCafe.controller.dto;

public class ShiftAssignRequest {
    private Long staffId;
    private String workDate; // yyyy-MM-dd
    private String shiftCode; // MORNING/AFTERNOON/EVENING

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getWorkDate() {
        return workDate;
    }

    public void setWorkDate(String workDate) {
        this.workDate = workDate;
    }

    public String getShiftCode() {
        return shiftCode;
    }

    public void setShiftCode(String shiftCode) {
        this.shiftCode = shiftCode;
    }
}


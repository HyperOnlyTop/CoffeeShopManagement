package com.example.QuanLyQuanCafe.controller.dto;

public class LoyaltyAdjustRequest {
    private String phone;
    private Integer delta;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getDelta() {
        return delta;
    }

    public void setDelta(Integer delta) {
        this.delta = delta;
    }
}


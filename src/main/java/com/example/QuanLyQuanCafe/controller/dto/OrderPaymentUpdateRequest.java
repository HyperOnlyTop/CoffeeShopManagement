package com.example.QuanLyQuanCafe.controller.dto;

public class OrderPaymentUpdateRequest {

    /** {@code true} = đã thu tiền (ghi nhận thời điểm); {@code false} = chưa thu (xóa ghi nhận). */
    private Boolean paid;

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }
}

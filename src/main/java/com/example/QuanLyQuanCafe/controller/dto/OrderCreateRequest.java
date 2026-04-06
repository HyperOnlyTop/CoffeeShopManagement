package com.example.QuanLyQuanCafe.controller.dto;

import java.util.List;

public class OrderCreateRequest {

    private String customerName;
    private String customerPhone;
    /** {@code true}: khách vãng lai — không SĐT, không tích/đổi điểm; tên hiển thị cố định phía server. */
    private Boolean walkInGuest;
    private String type;
    private Integer tableNumber;
    private String orderNote;
    private String status;
    private String paymentMethod;
    private List<OrderLineRequest> items;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public Boolean getWalkInGuest() {
        return walkInGuest;
    }

    public void setWalkInGuest(Boolean walkInGuest) {
        this.walkInGuest = walkInGuest;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(Integer tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getOrderNote() {
        return orderNote;
    }

    public void setOrderNote(String orderNote) {
        this.orderNote = orderNote;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public List<OrderLineRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderLineRequest> items) {
        this.items = items;
    }
}

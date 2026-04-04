package com.example.QuanLyQuanCafe.controller.dto;

public class OrderLineRequest {

    private Long menuItemId;
    private Integer quantity;
    private String note;
    /** Đúng 1 dòng / SL 1: đổi 10 điểm lấy 1 ly (đồ uống &lt; 50k). */
    private Boolean loyaltyRedemption;

    public Long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(Long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getLoyaltyRedemption() {
        return loyaltyRedemption;
    }

    public void setLoyaltyRedemption(Boolean loyaltyRedemption) {
        this.loyaltyRedemption = loyaltyRedemption;
    }
}

package com.example.QuanLyQuanCafe.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private CafeOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @Column(name = "item_name", length = 150)
    private String itemName;

    @Column(name = "item_price", precision = 15, scale = 0)
    private BigDecimal itemPrice;

    @Column(name = "item_cost", precision = 15, scale = 0)
    private BigDecimal itemCost;

    private Integer quantity;

    @Column(precision = 15, scale = 0)
    private BigDecimal subtotal;

    @Column(length = 255)
    private String note;

    /** Món đổi 10 điểm: không tính tiền, không cộng điểm tích khi hoàn thành đơn. */
    @Column(name = "loyalty_redemption", nullable = false)
    private Boolean loyaltyRedemption = Boolean.FALSE;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CafeOrder getOrder() {
        return order;
    }

    public void setOrder(CafeOrder order) {
        this.order = order;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    public BigDecimal getItemCost() {
        return itemCost;
    }

    public void setItemCost(BigDecimal itemCost) {
        this.itemCost = itemCost;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
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
        this.loyaltyRedemption = Boolean.TRUE.equals(loyaltyRedemption);
    }
}

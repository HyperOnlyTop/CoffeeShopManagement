package com.example.QuanLyQuanCafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "orders")
public class CafeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", length = 20, unique = true)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrderType type;

    @Column(name = "table_number")
    private Integer tableNumber;

    @Column(name = "order_note", length = 500)
    private String orderNote;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrderStatus status;

    @Column(name = "subtotal", precision = 15, scale = 0)
    private BigDecimal subtotal;

    @Column(name = "discount", precision = 15, scale = 0)
    private BigDecimal discount;

    @Column(name = "total", precision = 15, scale = 0)
    private BigDecimal total;

    @Convert(converter = PaymentMethodAttributeConverter.class)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Staff createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Thời điểm chuyển sang COMPLETED (hoàn thành pha chế) — dùng báo phục vụ / lọc. */
    @Column(name = "prepared_at")
    private LocalDateTime preparedAt;

    @Column(name = "loyalty_points_awarded")
    private Boolean loyaltyPointsAwarded;

    /**
     * {@code false} = khách vãng lai (SĐT chưa thuộc khách đã có trong hệ thống / chưa có tài khoản app với SĐT đó):
     * không trừ/cộng điểm khi hoàn thành đơn. {@code null} = đơn cũ trước khi có cột, giữ hành vi xử lý như trước.
     */
    @Column(name = "loyalty_earn_eligible")
    private Boolean loyaltyEarnEligible;

    @Column(name = "walk_in_guest")
    private Boolean walkInGuest;

    // Trả bàn là thao tác riêng ở màn Quản lý bàn.
    // Hoàn thành pha chế (COMPLETED) KHÔNG tự động trả bàn.
    @Column(name = "table_released")
    private Boolean tableReleased;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

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

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
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

    /** Cột danh sách đơn: mang về → {@code order_note}; tại bàn → {@code Bàn N} hoặc {@code Bàn N - ghi chú}. */
    @JsonIgnore
    public String getTableAndNoteColumn() {
        if (type == OrderType.TAKEAWAY) {
            return (orderNote != null && !orderNote.isBlank()) ? orderNote : "—";
        }
        if (type == OrderType.DINE_IN) {
            if (tableNumber == null && (orderNote == null || orderNote.isBlank())) {
                return "—";
            }
            if (tableNumber == null) {
                return orderNote;
            }
            if (orderNote == null || orderNote.isBlank()) {
                return "Bàn " + tableNumber;
            }
            return "Bàn " + tableNumber + " - " + orderNote;
        }
        return "—";
    }

    /** Sắp xếp cột bàn (ưu tiên số bàn). */
    @JsonIgnore
    public String getTableSortKey() {
        if (tableNumber != null) {
            return String.format(Locale.ROOT, "%04d", tableNumber);
        }
        if (orderNote != null && !orderNote.isBlank()) {
            return "z" + orderNote.toLowerCase(Locale.ROOT);
        }
        return "z";
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public Staff getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Staff createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPreparedAt() {
        return preparedAt;
    }

    public void setPreparedAt(LocalDateTime preparedAt) {
        this.preparedAt = preparedAt;
    }

    public Boolean getLoyaltyPointsAwarded() {
        return loyaltyPointsAwarded;
    }

    public void setLoyaltyPointsAwarded(Boolean loyaltyPointsAwarded) {
        this.loyaltyPointsAwarded = loyaltyPointsAwarded;
    }

    public Boolean getLoyaltyEarnEligible() {
        return loyaltyEarnEligible;
    }

    public void setLoyaltyEarnEligible(Boolean loyaltyEarnEligible) {
        this.loyaltyEarnEligible = loyaltyEarnEligible;
    }

    public Boolean getWalkInGuest() {
        return walkInGuest;
    }

    public void setWalkInGuest(Boolean walkInGuest) {
        this.walkInGuest = walkInGuest;
    }

    public Boolean getTableReleased() {
        return tableReleased;
    }

    public void setTableReleased(Boolean tableReleased) {
        this.tableReleased = tableReleased;
    }
}
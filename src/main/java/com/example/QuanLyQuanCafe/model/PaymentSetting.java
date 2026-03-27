package com.example.QuanLyQuanCafe.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
@Table(name = "payment_settings")
public class PaymentSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    @JsonIgnore
    private ShopSetting shop;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account", length = 30)
    private String bankAccount;

    @Column(name = "bank_owner_name", length = 100)
    private String bankOwnerName;

    @Column(name = "momo_phone", length = 15)
    private String momoPhone;

    @Column(name = "momo_owner_name", length = 100)
    private String momoOwnerName;

    @Column(name = "bank_enabled")
    private Boolean bankEnabled;

    @Column(name = "momo_enabled")
    private Boolean momoEnabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ShopSetting getShop() {
        return shop;
    }

    public void setShop(ShopSetting shop) {
        this.shop = shop;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getBankOwnerName() {
        return bankOwnerName;
    }

    public void setBankOwnerName(String bankOwnerName) {
        this.bankOwnerName = bankOwnerName;
    }

    public String getMomoPhone() {
        return momoPhone;
    }

    public void setMomoPhone(String momoPhone) {
        this.momoPhone = momoPhone;
    }

    public String getMomoOwnerName() {
        return momoOwnerName;
    }

    public void setMomoOwnerName(String momoOwnerName) {
        this.momoOwnerName = momoOwnerName;
    }

    public Boolean getBankEnabled() {
        return bankEnabled;
    }

    public void setBankEnabled(Boolean bankEnabled) {
        this.bankEnabled = bankEnabled;
    }

    public Boolean getMomoEnabled() {
        return momoEnabled;
    }

    public void setMomoEnabled(Boolean momoEnabled) {
        this.momoEnabled = momoEnabled;
    }
}

package com.example.QuanLyQuanCafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuanLyQuanCafe.model.PaymentSetting;

public interface PaymentSettingRepository extends JpaRepository<PaymentSetting, Long> {
}

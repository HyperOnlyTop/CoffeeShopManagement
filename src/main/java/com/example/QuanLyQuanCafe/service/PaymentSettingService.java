package com.example.QuanLyQuanCafe.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.QuanLyQuanCafe.model.PaymentSetting;
import com.example.QuanLyQuanCafe.model.ShopSetting;
import com.example.QuanLyQuanCafe.repository.PaymentSettingRepository;

@Service
public class PaymentSettingService {

    private final PaymentSettingRepository paymentSettingRepository;

    public PaymentSettingService(PaymentSettingRepository paymentSettingRepository) {
        this.paymentSettingRepository = paymentSettingRepository;
    }

    public List<PaymentSetting> findAll() {
        return paymentSettingRepository.findAll();
    }

    public PaymentSetting save(PaymentSetting setting) {
        return paymentSettingRepository.save(setting);
    }
}

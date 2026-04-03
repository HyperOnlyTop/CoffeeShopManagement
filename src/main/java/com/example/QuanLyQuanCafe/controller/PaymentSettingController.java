package com.example.QuanLyQuanCafe.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.model.PaymentSetting;
import com.example.QuanLyQuanCafe.model.ShopSetting;
import com.example.QuanLyQuanCafe.service.PaymentSettingService;
import com.example.QuanLyQuanCafe.service.ShopSettingService;

@RestController
@RequestMapping("/api/settings")
public class PaymentSettingController {

    private final PaymentSettingService paymentSettingService;
    private final ShopSettingService shopSettingService;

    public PaymentSettingController(PaymentSettingService paymentSettingService, ShopSettingService shopSettingService) {
        this.paymentSettingService = paymentSettingService;
        this.shopSettingService = shopSettingService;
    }

    @GetMapping("/payment")
    public ResponseEntity<PaymentSetting> getPaymentSetting() {
        List<PaymentSetting> all = paymentSettingService.findAll();
        if (all.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(all.get(0));
    }

    @PutMapping("/payment")
    public ResponseEntity<PaymentSetting> updatePaymentSetting(@RequestBody PaymentSetting incoming) {
        List<PaymentSetting> all = paymentSettingService.findAll();
        PaymentSetting current = all.isEmpty() ? new PaymentSetting() : all.get(0);

        ShopSetting shop = shopSettingService.getCurrentSetting();
        current.setShop(shop);

        current.setBankCode(incoming.getBankCode());
        current.setBankName(incoming.getBankName());
        current.setBankAccount(incoming.getBankAccount());
        current.setBankOwnerName(incoming.getBankOwnerName());
        current.setBankEnabled(incoming.getBankEnabled());

        PaymentSetting saved = paymentSettingService.save(current);
        return ResponseEntity.ok(saved);
    }
}

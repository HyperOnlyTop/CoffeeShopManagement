package com.example.QuanLyQuanCafe.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.model.ShopSetting;
import com.example.QuanLyQuanCafe.service.ShopSettingService;

@RestController
@RequestMapping("/api/settings")
public class ShopSettingController {

    private final ShopSettingService shopSettingService;

    public ShopSettingController(ShopSettingService shopSettingService) {
        this.shopSettingService = shopSettingService;
    }

    @GetMapping("/shop")
    public ResponseEntity<ShopSetting> getShopSetting() {
        ShopSetting setting = shopSettingService.getCurrentSetting();
        if (setting == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(setting);
    }

    @PutMapping("/shop")
    public ResponseEntity<ShopSetting> updateShopSetting(@RequestBody ShopSetting setting) {
        ShopSetting current = shopSettingService.getCurrentSetting();

        if (current == null) {
            current = new ShopSetting();
        }

        current.setShopName(setting.getShopName());
        current.setAddress(setting.getAddress());
        current.setPhone(setting.getPhone());
        current.setEmail(setting.getEmail());
        current.setWebsite(setting.getWebsite());
        current.setTaxCode(setting.getTaxCode());
        current.setDescription(setting.getDescription());
        current.setWeekdayOpen(setting.getWeekdayOpen());
        current.setWeekdayClose(setting.getWeekdayClose());
        current.setSaturdayOpen(setting.getSaturdayOpen());
        current.setSaturdayClose(setting.getSaturdayClose());
        current.setSundayOpen(setting.getSundayOpen());
        current.setSundayClose(setting.getSundayClose());

        ShopSetting saved = shopSettingService.save(current);
        return ResponseEntity.ok(saved);
    }
}

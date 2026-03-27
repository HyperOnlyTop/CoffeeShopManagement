package com.example.QuanLyQuanCafe.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.QuanLyQuanCafe.model.ShopSetting;
import com.example.QuanLyQuanCafe.repository.ShopSettingRepository;

@Service
public class ShopSettingService {

    private final ShopSettingRepository shopSettingRepository;

    public ShopSettingService(ShopSettingRepository shopSettingRepository) {
        this.shopSettingRepository = shopSettingRepository;
    }

    public ShopSetting getCurrentSetting() {
        List<ShopSetting> all = shopSettingRepository.findAll();
        return all.isEmpty() ? null : all.get(0);
    }

    public ShopSetting save(ShopSetting setting) {
        return shopSettingRepository.save(setting);
    }
}

package com.example.QuanLyQuanCafe.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.QuanLyQuanCafe.repository.MenuItemRepository;

@Component
public class MenuItemDateBackfill implements ApplicationRunner {

    private final MenuItemRepository menuItemRepository;

    public MenuItemDateBackfill(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        menuItemRepository.backfillNullTimestamps();
    }
}
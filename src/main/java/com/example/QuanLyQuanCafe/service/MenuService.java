package com.example.QuanLyQuanCafe.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.QuanLyQuanCafe.controller.dto.MenuItemRequest;
import com.example.QuanLyQuanCafe.model.MenuCategory;
import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.model.MenuItemStatus;
import com.example.QuanLyQuanCafe.repository.MenuCategoryRepository;
import com.example.QuanLyQuanCafe.repository.MenuItemRepository;

@Service
public class MenuService {

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;

    public MenuService(MenuCategoryRepository menuCategoryRepository, MenuItemRepository menuItemRepository) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public List<MenuCategory> getAllCategories() {
        return menuCategoryRepository.findAll();
    }

    public List<MenuItem> getAllItems() {
        return menuItemRepository.findAll();
    }

    private MenuCategory getOrCreateCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return null;
        }

        return menuCategoryRepository.findByName(categoryName.trim())
                .orElseGet(() -> {
                    MenuCategory category = new MenuCategory();
                    category.setName(categoryName.trim());

                    String base = Normalizer.normalize(categoryName, Normalizer.Form.NFD)
                            .replaceAll("[^\\p{ASCII}]", "")
                            .toLowerCase(Locale.ROOT)
                            .replaceAll("[^a-z0-9]+", "-")
                            .replaceAll("(^-|-$)", "");
                    category.setSlug(base);
                    category.setActive(Boolean.TRUE);
                    return menuCategoryRepository.save(category);
                });
    }

    public MenuItem saveFromRequest(MenuItemRequest request) {
        if (request == null || request.getName() == null) {
            throw new IllegalArgumentException("Tên món không được để trống");
        }

        MenuItem item = menuItemRepository.findByName(request.getName()).orElseGet(MenuItem::new);

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCost(request.getCost());
        item.setImageUrl(request.getImageUrl());

        MenuCategory category = getOrCreateCategory(request.getCategoryName());
        if (category != null) {
            item.setCategory(category);
        }

        String statusStr = request.getStatus();
        if (statusStr != null) {
            String normalized = statusStr.trim().toUpperCase(Locale.ROOT);
            try {
                item.setStatus(MenuItemStatus.valueOf(normalized));
            } catch (IllegalArgumentException ex) {
                // ignore invalid status, keep current or default below
            }
        }

        if (item.getStatus() == null) {
            item.setStatus(MenuItemStatus.AVAILABLE);
        }

        return menuItemRepository.save(item);
    }

    public MenuItem updateStatusByName(String name, MenuItemStatus status) {
        if (name == null) {
            throw new IllegalArgumentException("Tên món không được để trống");
        }
        MenuItem item = menuItemRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món: " + name));
        item.setStatus(status);
        return menuItemRepository.save(item);
    }
}

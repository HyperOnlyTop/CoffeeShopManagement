package com.example.QuanLyQuanCafe.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.QuanLyQuanCafe.controller.dto.MenuItemRequest;
import com.example.QuanLyQuanCafe.model.MenuCategory;
import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.model.MenuItemStatus;
import com.example.QuanLyQuanCafe.repository.MenuCategoryRepository;
import com.example.QuanLyQuanCafe.repository.MenuItemRepository;
import com.example.QuanLyQuanCafe.repository.OrderItemRepository;

@Service
public class MenuService {

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderItemRepository orderItemRepository;

    public MenuService(MenuCategoryRepository menuCategoryRepository,
                       MenuItemRepository menuItemRepository,
                       OrderItemRepository orderItemRepository) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public List<MenuCategory> getAllCategories() {
        return menuCategoryRepository.findAll();
    }

    public List<MenuItem> getAllItems() {
        return menuItemRepository.findAll();
    }

    public List<MenuItem> getAvailableItems() {
        return menuItemRepository.findByStatus(MenuItemStatus.AVAILABLE);
    }

    public List<MenuItem> searchAvailableItemsByName(String query) {
        if (query == null) return List.of();
        String q = query.trim();
        if (q.length() < 2) return List.of();
        return menuItemRepository.findTop20ByStatusAndNameContainingIgnoreCaseOrderByNameAsc(MenuItemStatus.AVAILABLE, q);
    }

    public Page<MenuItem> getItemsPage(Pageable pageable) {
        return menuItemRepository.findAll(pageable);
    }

    public Page<MenuItem> getAvailableItemsPage(Pageable pageable) {
        return menuItemRepository.findByStatus(MenuItemStatus.AVAILABLE, pageable);
    }

    public Page<MenuItem> getAvailableItemsPageByCategory(String categoryName, Pageable pageable) {
        if (categoryName == null || categoryName.isBlank()) {
            return getAvailableItemsPage(pageable);
        }
        return menuItemRepository.findByStatusAndCategory_Name(MenuItemStatus.AVAILABLE, categoryName.trim(), pageable);
    }

    public List<MenuItem> getBestSellerAvailableItems(int limit) {
        int safeLimit = limit <= 0 ? 8 : Math.min(limit, 12);
        List<Object[]> bestSellerRows = orderItemRepository.findBestSellerMenuItemIds();
        if (bestSellerRows == null || bestSellerRows.isEmpty()) {
            return getAvailableItems().stream().limit(safeLimit).toList();
        }

        List<Long> ids = new ArrayList<>();
        for (Object[] row : bestSellerRows) {
            if (row != null && row.length >= 1 && row[0] instanceof Long id) {
                ids.add(id);
            }
            if (ids.size() >= safeLimit) {
                break;
            }
        }

        if (ids.isEmpty()) {
            return getAvailableItems().stream().limit(safeLimit).toList();
        }

        List<MenuItem> items = menuItemRepository.findAllById(ids).stream()
                .filter(mi -> mi != null && mi.getStatus() == MenuItemStatus.AVAILABLE)
                .collect(Collectors.toList());

        Map<Long, Integer> orderIndex = ids.stream()
                .collect(Collectors.toMap(id -> id, ids::indexOf, (a, b) -> a));

        items.sort((a, b) -> Integer.compare(
                orderIndex.getOrDefault(a.getId(), Integer.MAX_VALUE),
                orderIndex.getOrDefault(b.getId(), Integer.MAX_VALUE)
        ));
        return items;
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

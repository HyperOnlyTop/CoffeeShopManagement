package com.example.QuanLyQuanCafe.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.controller.dto.MenuItemRequest;
import com.example.QuanLyQuanCafe.model.MenuCategory;
import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.model.MenuItemStatus;
import com.example.QuanLyQuanCafe.service.MenuService;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/categories")
    public List<MenuCategory> getCategories() {
        return menuService.getAllCategories();
    }

    @GetMapping("/items")
    public List<MenuItem> getItems() {
        return menuService.getAllItems();
    }

    @GetMapping("/search")
    public List<MenuItem> search(@RequestParam(value = "q", required = false) String q) {
        return menuService.searchAvailableItemsByName(q);
    }

    @PostMapping("/items")
    public MenuItem createOrUpdateItem(@RequestBody MenuItemRequest request) {
        return menuService.saveFromRequest(request);
    }

    @PostMapping("/items/hide")
    public MenuItem hideItem(@RequestBody MenuItemRequest request) {
        String name = request != null ? request.getName() : null;
        return menuService.updateStatusByName(name, MenuItemStatus.UNAVAILABLE);
    }
}

package com.example.QuanLyQuanCafe.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.model.InventoryCategory;
import com.example.QuanLyQuanCafe.model.InventoryImportRequest;
import com.example.QuanLyQuanCafe.model.InventoryItem;
import com.example.QuanLyQuanCafe.model.InventoryStatus;
import com.example.QuanLyQuanCafe.model.InventoryUpdateRequest;
import com.example.QuanLyQuanCafe.service.InventoryService;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/categories")
    public List<InventoryCategory> getCategories() {
        return inventoryService.findAllCategoriesOrdered();
    }

    @GetMapping("/items")
    public List<InventoryItem> getAllItems() {
        return inventoryService.findAll();
    }

    @GetMapping("/items/status/{status}")
    public List<InventoryItem> getItemsByStatus(@PathVariable("status") InventoryStatus status) {
        return inventoryService.findByStatus(status);
    }

    @PostMapping("/import")
    public ResponseEntity<InventoryItem> importStock(@RequestBody InventoryImportRequest request) {
        InventoryItem item = inventoryService.importStock(request);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<InventoryItem> updateItem(@PathVariable("id") Long id,
            @RequestBody InventoryUpdateRequest request) {
        InventoryItem item = inventoryService.updateItem(id, request);
        return ResponseEntity.ok(item);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}

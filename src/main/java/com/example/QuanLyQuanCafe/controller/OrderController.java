package com.example.QuanLyQuanCafe.controller;

import java.time.LocalDateTime;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.OrderItem;
import com.example.QuanLyQuanCafe.model.OrderStatus;
import com.example.QuanLyQuanCafe.service.OrderService;
import com.example.QuanLyQuanCafe.controller.dto.OrderCreateRequest;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<CafeOrder> getAll() {
        return orderService.findAll();
    }

    @PostMapping
    public ResponseEntity<CafeOrder> create(@RequestBody OrderCreateRequest request) {
        CafeOrder created = orderService.createOrder(request);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{code}")
    public ResponseEntity<CafeOrder> getByCode(@PathVariable("code") String code) {
        CafeOrder order = orderService.findByOrderCode(code);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    @GetMapping("/status/{status}")
    public List<CafeOrder> getByStatus(@PathVariable("status") OrderStatus status) {
        return orderService.findByStatus(status);
    }

    @GetMapping("/range")
    public List<CafeOrder> getByCreatedRange(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return orderService.findByCreatedAtBetween(from, to);
    }

    @PutMapping("/{code}")
    public ResponseEntity<CafeOrder> update(@PathVariable("code") String code, @RequestBody OrderCreateRequest request) {
        CafeOrder updated = orderService.updateOrder(code, request);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{code}/items")
    public ResponseEntity<List<Map<String, Object>>> getOrderItems(@PathVariable("code") String code) {
        CafeOrder order = orderService.findByOrderCode(code);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        List<OrderItem> items = orderService.findItemsByOrder(order);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (OrderItem item : items) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("itemName", item.getItemName());
            map.put("itemPrice", item.getItemPrice());
            map.put("quantity", item.getQuantity());
            if (item.getMenuItem() != null) {
                map.put("menuItemId", item.getMenuItem().getId());
            }
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
}

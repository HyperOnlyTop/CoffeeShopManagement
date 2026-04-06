package com.example.QuanLyQuanCafe.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.example.QuanLyQuanCafe.controller.dto.OrderPaymentUpdateRequest;
import com.example.QuanLyQuanCafe.controller.dto.OrderStatusUpdateRequest;

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

    /**
     * Đơn vừa hoàn thành pha chế ({@code preparedAt} &gt; after) — phục vụ poll để báo lấy nước.
     */
    @GetMapping("/prepared-since")
    public ResponseEntity<List<Map<String, Object>>> getPreparedSince(
            @RequestParam("after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after) {
        List<CafeOrder> list = orderService.findCompletedPreparedAfter(after);
        List<Map<String, Object>> out = new ArrayList<>();
        for (CafeOrder o : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("orderCode", o.getOrderCode());
            m.put("type", o.getType() != null ? o.getType().name() : null);
            m.put("tableNumber", o.getTableNumber());
            m.put("customerName", o.getCustomerName());
            m.put("orderNote", o.getOrderNote());
            m.put("preparedAt", o.getPreparedAt() != null ? o.getPreparedAt().toString() : null);
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody OrderCreateRequest request) {
        try {
            CafeOrder created = orderService.createOrder(request);
            return ResponseEntity.ok(created);
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
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
    public ResponseEntity<?> update(@PathVariable("code") String code, @RequestBody OrderCreateRequest request) {
        try {
            CafeOrder updated = orderService.updateOrder(code, request);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/{code}/status")
    public ResponseEntity<?> updateStatus(@PathVariable("code") String code, @RequestBody OrderStatusUpdateRequest request) {
        CafeOrder order = orderService.findByOrderCode(code);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        String statusStr = request != null ? request.getStatus() : null;
        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing status"));
        }

        OrderStatus next;
        try {
            next = OrderStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status"));
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isBarista = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_BARISTA"));
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isCashier = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CASHIER"));

        // Barista chỉ được chuyển sang COMPLETED (hoàn thành pha chế)
        if (isBarista && next != OrderStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "BARISTA only allowed COMPLETED"));
        }
        // COMPLETED: chỉ Admin hoặc Pha chế (thu ngân / phục vụ không được)
        if (next == OrderStatus.COMPLETED) {
            if (!isBarista && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Chỉ Admin hoặc Pha chế được đánh dấu hoàn thành pha chế."));
            }
        } else {
            if (!isAdmin && !isCashier) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        CafeOrder saved = orderService.updateOrderStatus(order, next);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{code}/payment")
    public ResponseEntity<?> updatePayment(@PathVariable("code") String code, @RequestBody OrderPaymentUpdateRequest request) {
        if (request == null || request.getPaid() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Thiếu trường paid (true/false)."));
        }
        try {
            CafeOrder updated = orderService.setOrderPaid(code, Boolean.TRUE.equals(request.getPaid()));
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
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
            map.put("note", item.getNote());
            if (item.getMenuItem() != null) {
                map.put("menuItemId", item.getMenuItem().getId());
            }
            map.put("loyaltyRedemption", Boolean.TRUE.equals(item.getLoyaltyRedemption()));
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleOrderBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}

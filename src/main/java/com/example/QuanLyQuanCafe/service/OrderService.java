package com.example.QuanLyQuanCafe.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.QuanLyQuanCafe.controller.dto.OrderCreateRequest;
import com.example.QuanLyQuanCafe.controller.dto.OrderLineRequest;
import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.model.OrderItem;
import com.example.QuanLyQuanCafe.model.OrderStatus;
import com.example.QuanLyQuanCafe.model.OrderType;
import com.example.QuanLyQuanCafe.model.PaymentMethod;
import com.example.QuanLyQuanCafe.repository.CafeOrderRepository;
import com.example.QuanLyQuanCafe.repository.MenuItemRepository;
import com.example.QuanLyQuanCafe.repository.OrderItemRepository;

@Service
public class OrderService {

    private final CafeOrderRepository cafeOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;

    public OrderService(CafeOrderRepository cafeOrderRepository,
            OrderItemRepository orderItemRepository,
            MenuItemRepository menuItemRepository) {
        this.cafeOrderRepository = cafeOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public List<CafeOrder> findAll() {
        return cafeOrderRepository.findAll();
    }

    public long countAll() {
        return cafeOrderRepository.count();
    }

    public CafeOrder findByOrderCode(String code) {
        return cafeOrderRepository.findByOrderCode(code);
    }

    public List<CafeOrder> findByStatus(OrderStatus status) {
        return cafeOrderRepository.findByStatus(status);
    }

    public List<CafeOrder> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        return cafeOrderRepository.findByCreatedAtBetween(start, end);
    }

    public List<CafeOrder> findRecent(int limit) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                0,
                limit,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return cafeOrderRepository.findAll(pageable).getContent();
    }

    public List<OrderItem> findItemsByOrder(CafeOrder order) {
        return orderItemRepository.findByOrder(order);
    }

    public CafeOrder save(CafeOrder order) {
        return cafeOrderRepository.save(order);
    }

    @Transactional
    public CafeOrder createOrder(OrderCreateRequest request) {
        CafeOrder order = new CafeOrder();

        order.setOrderCode(generateOrderCode());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());

        OrderType type = null;
        if (request.getType() != null) {
            try {
                type = OrderType.valueOf(request.getType());
            } catch (IllegalArgumentException ex) {
                // ignore, will use default
            }
        }
        if (type == null) {
            type = OrderType.DINE_IN;
        }
        order.setType(type);

        order.setTableName(request.getTableNote());

        OrderStatus status = OrderStatus.PENDING;
        if (request.getStatus() != null) {
            try {
                status = OrderStatus.valueOf(request.getStatus());
            } catch (IllegalArgumentException ex) {
                // keep default PENDING
            }
        }
        order.setStatus(status);

        PaymentMethod paymentMethod = PaymentMethod.CASH;
        if (request.getPaymentMethod() != null) {
            try {
                paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod());
            } catch (IllegalArgumentException ex) {
                // keep default CASH
            }
        }
        order.setPaymentMethod(paymentMethod);

        order.setCreatedAt(LocalDateTime.now());

        // Initial save to get ID
        CafeOrder savedOrder = cafeOrderRepository.save(order);

        BigDecimal subtotal = BigDecimal.ZERO;

        if (request.getItems() != null) {
            for (OrderLineRequest line : request.getItems()) {
                if (line.getMenuItemId() == null) {
                    continue;
                }

                MenuItem menuItem = menuItemRepository.findById(line.getMenuItemId()).orElse(null);
                if (menuItem == null) {
                    continue;
                }

                int quantity = (line.getQuantity() != null && line.getQuantity() > 0) ? line.getQuantity() : 1;

                BigDecimal price = menuItem.getPrice() != null ? menuItem.getPrice() : BigDecimal.ZERO;
                BigDecimal cost = menuItem.getCost() != null ? menuItem.getCost() : BigDecimal.ZERO;
                BigDecimal lineSubtotal = price.multiply(BigDecimal.valueOf(quantity));

                subtotal = subtotal.add(lineSubtotal);

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder);
                orderItem.setMenuItem(menuItem);
                orderItem.setItemName(menuItem.getName());
                orderItem.setItemPrice(price);
                orderItem.setItemCost(cost);
                orderItem.setQuantity(quantity);
                orderItem.setSubtotal(lineSubtotal);

                orderItemRepository.save(orderItem);
            }
        }

        order.setSubtotal(subtotal);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(subtotal);

        return cafeOrderRepository.save(order);
    }

    private String generateOrderCode() {
        long randomPart = ThreadLocalRandom.current().nextLong(1000, 9999);
        long timestampPart = System.currentTimeMillis() % 10000;
        return "ORD-" + randomPart + timestampPart;
    }
}

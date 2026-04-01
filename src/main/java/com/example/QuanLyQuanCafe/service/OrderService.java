package com.example.QuanLyQuanCafe.service;

import com.example.QuanLyQuanCafe.model.Customer;
import com.example.QuanLyQuanCafe.repository.CustomerRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Set;

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
    private final CustomerRepository customerRepository;

    public OrderService(CafeOrderRepository cafeOrderRepository,
            OrderItemRepository orderItemRepository,
            MenuItemRepository menuItemRepository,
            CustomerRepository customerRepository) {
        this.cafeOrderRepository = cafeOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.customerRepository = customerRepository;
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

    private static final Set<String> NON_DRINK_CATEGORIES = Set.of(
            "bánh ngọt",
            "thức ăn nhẹ",
            "đồ ăn nhẹ",
            "do an nhe",
            "banh ngot",
            "food",
            "snack"
    );

    private boolean isDrinkMenuItem(MenuItem menuItem) {
        if (menuItem == null) return false;
        if (menuItem.getCategory() == null || menuItem.getCategory().getName() == null) {
            return true; // không có danh mục thì tạm coi là đồ uống để không mất điểm
        }
        String cat = menuItem.getCategory().getName().trim().toLowerCase();
        return !NON_DRINK_CATEGORIES.contains(cat);
    }

    private void maybeAwardLoyaltyPoints(CafeOrder order, List<OrderItem> orderItems) {
        if (order == null) return;
        if (order.getStatus() != OrderStatus.COMPLETED) return;
        if (Boolean.TRUE.equals(order.getLoyaltyPointsAwarded())) return;

        String phone = order.getCustomerPhone();
        if (phone == null || phone.isBlank()) return;

        Customer customer = customerRepository.findByPhone(phone.trim()).orElse(null);
        if (customer == null) return;

        int earned = 0;
        if (orderItems != null) {
            for (OrderItem oi : orderItems) {
                if (oi == null) continue;
                int qty = oi.getQuantity() != null ? oi.getQuantity() : 0;
                if (qty <= 0) continue;
                MenuItem mi = oi.getMenuItem();
                if (isDrinkMenuItem(mi)) {
                    earned += qty;
                }
            }
        }
        if (earned <= 0) {
            order.setLoyaltyPointsAwarded(Boolean.TRUE);
            cafeOrderRepository.save(order);
            return;
        }

        int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        customer.setLoyaltyPoints(currentPoints + earned);
        customerRepository.save(customer);

        order.setLoyaltyPointsAwarded(Boolean.TRUE);
        cafeOrderRepository.save(order);
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
        order.setLoyaltyPointsAwarded(Boolean.FALSE);
        order.setTableReleased(Boolean.FALSE);

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
                orderItem.setNote(line.getNote());

                orderItemRepository.save(orderItem);
            }
        }

        order.setSubtotal(subtotal);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(subtotal);

        CafeOrder finalOrder = cafeOrderRepository.save(order);

        if (request.getCustomerPhone() != null && !request.getCustomerPhone().trim().isEmpty()) {
            String phone = request.getCustomerPhone().trim();
            Customer cust = customerRepository.findByPhone(phone).orElse(null);
            
            if (cust != null) {
                cust.setTotalOrders((cust.getTotalOrders() != null ? cust.getTotalOrders() : 0) + 1);
                BigDecimal currentSpent = cust.getTotalSpent() != null ? cust.getTotalSpent() : BigDecimal.ZERO;
                cust.setTotalSpent(currentSpent.add(finalOrder.getTotal()));
                if (request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty()) {
                    cust.setName(request.getCustomerName().trim()); // Update name if new one provided
                }
            } else {
                cust = new Customer();
                cust.setPhone(phone);
                cust.setName(request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty() ? request.getCustomerName().trim() : "Khách mới");
                cust.setTotalOrders(1);
                cust.setTotalSpent(finalOrder.getTotal());
                cust.setLoyaltyPoints(0);
                cust.setLoyaltyRedeemedCount(0);
            }
            customerRepository.save(cust);
        }

        // Nếu đơn tạo ra đã là COMPLETED thì cộng điểm ngay (1 ly = 1 điểm, theo SĐT)
        List<OrderItem> finalItems = orderItemRepository.findByOrder(finalOrder);
        maybeAwardLoyaltyPoints(finalOrder, finalItems);
        return finalOrder;
    }

    @Transactional
    public CafeOrder updateOrder(String code, OrderCreateRequest request) {
        CafeOrder order = cafeOrderRepository.findByOrderCode(code);
        if (order == null) {
            return null;
        }

        OrderStatus previousStatus = order.getStatus();

        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());

        OrderType type = null;
        if (request.getType() != null) {
            try {
                type = OrderType.valueOf(request.getType());
            } catch (IllegalArgumentException ex) {
                // ignore
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

        // Delete existing items
        List<OrderItem> existingItems = orderItemRepository.findByOrder(order);
        orderItemRepository.deleteAll(existingItems);

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
                orderItem.setOrder(order);
                orderItem.setMenuItem(menuItem);
                orderItem.setItemName(menuItem.getName());
                orderItem.setItemPrice(price);
                orderItem.setItemCost(cost);
                orderItem.setQuantity(quantity);
                orderItem.setSubtotal(lineSubtotal);
                orderItem.setNote(line.getNote());

                orderItemRepository.save(orderItem);
            }
        }

        order.setSubtotal(subtotal);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(subtotal);

        CafeOrder saved = cafeOrderRepository.save(order);

        // Nếu chuyển sang COMPLETED và chưa cộng điểm thì cộng 1 lần
        if (saved.getStatus() == OrderStatus.COMPLETED && previousStatus != OrderStatus.COMPLETED) {
            List<OrderItem> finalItems = orderItemRepository.findByOrder(saved);
            maybeAwardLoyaltyPoints(saved, finalItems);
        }

        return saved;
    }

    private String generateOrderCode() {
        long randomPart = ThreadLocalRandom.current().nextLong(1000, 9999);
        long timestampPart = System.currentTimeMillis() % 10000;
        return "ORD-" + randomPart + timestampPart;
    }
}

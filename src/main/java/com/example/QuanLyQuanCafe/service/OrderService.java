package com.example.QuanLyQuanCafe.service;

import com.example.QuanLyQuanCafe.model.Customer;
import com.example.QuanLyQuanCafe.repository.CustomerRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.QuanLyQuanCafe.config.LoyaltyPolicy;
import com.example.QuanLyQuanCafe.controller.dto.OrderCreateRequest;
import com.example.QuanLyQuanCafe.controller.dto.OrderLineRequest;
import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.model.OrderItem;
import com.example.QuanLyQuanCafe.model.OrderStatus;
import com.example.QuanLyQuanCafe.model.OrderType;
import com.example.QuanLyQuanCafe.model.PaymentMethod;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.repository.CafeOrderRepository;
import com.example.QuanLyQuanCafe.repository.MenuItemRepository;
import com.example.QuanLyQuanCafe.repository.OrderItemRepository;

@Service
public class OrderService {

    private static final String WALK_IN_DISPLAY_NAME = "Khách vãng lai";

    private final CafeOrderRepository cafeOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final CustomerRepository customerRepository;
    private final AppUserRepository appUserRepository;

    public OrderService(CafeOrderRepository cafeOrderRepository,
            OrderItemRepository orderItemRepository,
            MenuItemRepository menuItemRepository,
            CustomerRepository customerRepository,
            AppUserRepository appUserRepository) {
        this.cafeOrderRepository = cafeOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.customerRepository = customerRepository;
        this.appUserRepository = appUserRepository;
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

    /**
     * Thu tiền do nhân viên xác nhận — độc lập với trạng thái pha chế / trả bàn.
     */
    @Transactional
    public CafeOrder setOrderPaid(String orderCode, boolean paid) {
        CafeOrder order = cafeOrderRepository.findByOrderCode(orderCode);
        if (order == null) {
            return null;
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Đơn đã hủy, không cập nhật thanh toán.");
        }
        if (paid) {
            order.setPaidAt(LocalDateTime.now());
        } else {
            order.setPaidAt(null);
        }
        return cafeOrderRepository.save(order);
    }

    @Transactional
    public CafeOrder updateOrderStatus(CafeOrder order, OrderStatus next) {
        OrderStatus previous = order.getStatus();
        order.setStatus(next);
        if (next == OrderStatus.COMPLETED && previous != OrderStatus.COMPLETED) {
            order.setPreparedAt(LocalDateTime.now());
        }
        CafeOrder saved = cafeOrderRepository.save(order);
        if (saved.getStatus() == OrderStatus.COMPLETED && previous != OrderStatus.COMPLETED) {
            maybeAwardLoyaltyPoints(saved, orderItemRepository.findByOrder(saved));
        }
        return saved;
    }

    public List<CafeOrder> findCompletedPreparedAfter(LocalDateTime after) {
        if (after == null) {
            return List.of();
        }
        return cafeOrderRepository.findTop30ByStatusAndPreparedAtAfterOrderByPreparedAtAsc(OrderStatus.COMPLETED, after);
    }

    /** Đơn dine-in mới nhất còn chiếm bàn (theo {@code table_number}). */
    public CafeOrder findActiveDineInOrderForTable(int tableNo) {
        if (tableNo < 1) {
            return null;
        }
        List<CafeOrder> orders = findAll();
        CafeOrder picked = null;
        for (CafeOrder o : orders) {
            if (o == null) continue;
            if (o.getType() != OrderType.DINE_IN) continue;
            if (o.getStatus() == OrderStatus.CANCELLED) continue;
            if (Boolean.TRUE.equals(o.getTableReleased())) continue;

            Integer no = o.getTableNumber();
            if (no == null || no != tableNo) continue;

            if (picked == null) {
                picked = o;
                continue;
            }
            if (picked.getCreatedAt() != null && o.getCreatedAt() != null && o.getCreatedAt().isAfter(picked.getCreatedAt())) {
                picked = o;
            }
        }
        return picked;
    }

    private static String trimOrderNoteField(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > 500) {
            return t.substring(0, 500);
        }
        return t;
    }

    /**
     * Khách vãng lai: bỏ SĐT, tên hiển thị cố định (client không thể gửi SĐT để lách tích điểm).
     */
    private void normalizeWalkInOnRequest(OrderCreateRequest request) {
        if (!Boolean.TRUE.equals(request.getWalkInGuest())) {
            return;
        }
        request.setCustomerPhone(null);
        request.setCustomerName(WALK_IN_DISPLAY_NAME);
    }

    private void assertValidLoyaltyRedemptions(OrderCreateRequest request) {
        if (request.getItems() == null) {
            return;
        }
        int redemptionLines = 0;
        int redemptionQty = 0;
        for (OrderLineRequest line : request.getItems()) {
            if (!Boolean.TRUE.equals(line.getLoyaltyRedemption())) {
                continue;
            }
            redemptionLines++;
            int q = (line.getQuantity() != null && line.getQuantity() > 0) ? line.getQuantity() : 1;
            redemptionQty += q;
        }
        if (redemptionLines == 0) {
            return;
        }
        if (Boolean.TRUE.equals(request.getWalkInGuest())) {
            throw new IllegalArgumentException("Khách vãng lai không thể đổi điểm.");
        }
        if (redemptionLines != 1 || redemptionQty != 1) {
            throw new IllegalArgumentException("Đổi điểm: chỉ được một dòng món, số lượng 1 ly.");
        }
        String phone = request.getCustomerPhone();
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Nhập SĐT khách để đổi điểm.");
        }
        Customer c = customerRepository.findByPhone(phone.trim()).orElse(null);
        if (c == null) {
            throw new IllegalArgumentException("Khách chưa có trong hệ thống (chưa lưu SĐT).");
        }
        int pts = c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0;
        if (pts < LoyaltyPolicy.POINTS_PER_FREE_DRINK) {
            throw new IllegalArgumentException("Khách không đủ " + LoyaltyPolicy.POINTS_PER_FREE_DRINK + " điểm để đổi.");
        }
        for (OrderLineRequest line : request.getItems()) {
            if (!Boolean.TRUE.equals(line.getLoyaltyRedemption())) {
                continue;
            }
            MenuItem mi = menuItemRepository.findById(line.getMenuItemId()).orElse(null);
            if (mi == null) {
                throw new IllegalArgumentException("Món đổi điểm không tồn tại.");
            }
            if (!isDrinkMenuItem(mi)) {
                throw new IllegalArgumentException("Chỉ đồ uống mới được đổi bằng điểm.");
            }
            BigDecimal p = mi.getPrice() != null ? mi.getPrice() : BigDecimal.ZERO;
            if (p.compareTo(LoyaltyPolicy.MAX_MENU_PRICE_FREE_DRINK_VND) >= 0) {
                throw new IllegalArgumentException("Món đổi điểm phải có giá dưới 50.000đ.");
            }
        }
    }

    /** @return tiền dòng cộng vào tổng đơn (0 nếu đổi điểm). */
    private BigDecimal persistOrderLine(CafeOrder order, OrderLineRequest line) {
        if (line.getMenuItemId() == null) {
            return BigDecimal.ZERO;
        }
        MenuItem menuItem = menuItemRepository.findById(line.getMenuItemId()).orElse(null);
        if (menuItem == null) {
            return BigDecimal.ZERO;
        }

        int quantity = (line.getQuantity() != null && line.getQuantity() > 0) ? line.getQuantity() : 1;
        BigDecimal catalogPrice = menuItem.getPrice() != null ? menuItem.getPrice() : BigDecimal.ZERO;
        BigDecimal cost = menuItem.getCost() != null ? menuItem.getCost() : BigDecimal.ZERO;
        boolean redeem = Boolean.TRUE.equals(line.getLoyaltyRedemption());

        BigDecimal unitPrice;
        BigDecimal lineSubtotal;
        if (redeem) {
            unitPrice = BigDecimal.ZERO;
            lineSubtotal = BigDecimal.ZERO;
        } else {
            unitPrice = catalogPrice;
            lineSubtotal = catalogPrice.multiply(BigDecimal.valueOf(quantity));
        }

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setMenuItem(menuItem);
        orderItem.setItemName(redeem ? (menuItem.getName() + " (đổi điểm)") : menuItem.getName());
        orderItem.setItemPrice(unitPrice);
        orderItem.setItemCost(cost);
        orderItem.setQuantity(quantity);
        orderItem.setSubtotal(lineSubtotal);
        orderItem.setNote(line.getNote());
        orderItem.setLoyaltyRedemption(redeem);
        orderItemRepository.save(orderItem);
        return lineSubtotal;
    }

    private void applyTableAndNoteFromRequest(CafeOrder order, OrderType type, OrderCreateRequest request) {
        if (type == OrderType.TAKEAWAY) {
            order.setTableNumber(null);
            order.setOrderNote(trimOrderNoteField(request.getOrderNote()));
            return;
        }
        order.setTableNumber(request.getTableNumber());
        order.setOrderNote(trimOrderNoteField(request.getOrderNote()));
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
            return false;
        }
        String cat = menuItem.getCategory().getName().trim().toLowerCase();
        return !NON_DRINK_CATEGORIES.contains(cat);
    }

    /**
     * Chỉ khách đã có hồ sơ (customers) hoặc đã đăng ký app (users.phone) trước đơn mới được tích/trừ điểm.
     */
    private boolean computeLoyaltyEarnEligibility(String customerPhone) {
        if (customerPhone == null || customerPhone.isBlank()) {
            return false;
        }
        String phone = customerPhone.trim();
        if (appUserRepository.existsByPhone(phone)) {
            return true;
        }
        return customerRepository.findByPhone(phone).isPresent();
    }

    private static boolean orderAllowsLoyaltyProcessing(CafeOrder order) {
        Boolean f = order.getLoyaltyEarnEligible();
        return f == null || Boolean.TRUE.equals(f);
    }

    private void maybeAwardLoyaltyPoints(CafeOrder order, List<OrderItem> orderItems) {
        if (order == null) {
            return;
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            return;
        }
        if (Boolean.TRUE.equals(order.getLoyaltyPointsAwarded())) {
            return;
        }
        if (!orderAllowsLoyaltyProcessing(order)) {
            order.setLoyaltyPointsAwarded(Boolean.TRUE);
            cafeOrderRepository.save(order);
            return;
        }

        String phone = order.getCustomerPhone();
        if (phone == null || phone.isBlank()) {
            order.setLoyaltyPointsAwarded(Boolean.TRUE);
            cafeOrderRepository.save(order);
            return;
        }

        Customer customer = customerRepository.findByPhone(phone.trim()).orElse(null);
        if (customer == null) {
            order.setLoyaltyPointsAwarded(Boolean.TRUE);
            cafeOrderRepository.save(order);
            return;
        }

        int redeemDrinks = 0;
        if (orderItems != null) {
            for (OrderItem oi : orderItems) {
                if (oi == null || !Boolean.TRUE.equals(oi.getLoyaltyRedemption())) {
                    continue;
                }
                int q = oi.getQuantity() != null ? oi.getQuantity() : 0;
                if (q <= 0) {
                    continue;
                }
                if (!isDrinkMenuItem(oi.getMenuItem())) {
                    continue;
                }
                redeemDrinks += q;
            }
        }

        int points = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        int redeemedCount = customer.getLoyaltyRedeemedCount() != null ? customer.getLoyaltyRedeemedCount() : 0;

        if (redeemDrinks == 1 && points >= LoyaltyPolicy.POINTS_PER_FREE_DRINK) {
            points -= LoyaltyPolicy.POINTS_PER_FREE_DRINK;
            redeemedCount += 1;
        }

        int earned = 0;
        if (orderItems != null) {
            for (OrderItem oi : orderItems) {
                if (oi == null || Boolean.TRUE.equals(oi.getLoyaltyRedemption())) {
                    continue;
                }
                int qty = oi.getQuantity() != null ? oi.getQuantity() : 0;
                if (qty <= 0) {
                    continue;
                }
                if (isDrinkMenuItem(oi.getMenuItem())) {
                    earned += qty;
                }
            }
        }

        points += earned;
        customer.setLoyaltyPoints(points);
        customer.setLoyaltyRedeemedCount(redeemedCount);
        customerRepository.save(customer);

        order.setLoyaltyPointsAwarded(Boolean.TRUE);
        cafeOrderRepository.save(order);
    }

    @Transactional
    public CafeOrder createOrder(OrderCreateRequest request) {
        normalizeWalkInOnRequest(request);

        CafeOrder order = new CafeOrder();

        order.setOrderCode(generateOrderCode());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setWalkInGuest(Boolean.TRUE.equals(request.getWalkInGuest()) ? Boolean.TRUE : Boolean.FALSE);

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

        applyTableAndNoteFromRequest(order, type, request);

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
        order.setLoyaltyEarnEligible(Boolean.TRUE.equals(request.getWalkInGuest())
                ? Boolean.FALSE
                : computeLoyaltyEarnEligibility(request.getCustomerPhone()));

        // Initial save to get ID
        CafeOrder savedOrder = cafeOrderRepository.save(order);

        assertValidLoyaltyRedemptions(request);

        BigDecimal subtotal = BigDecimal.ZERO;
        if (request.getItems() != null) {
            for (OrderLineRequest line : request.getItems()) {
                subtotal = subtotal.add(persistOrderLine(savedOrder, line));
            }
        }

        order.setSubtotal(subtotal);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(subtotal);

        if (order.getStatus() == OrderStatus.COMPLETED && order.getPreparedAt() == null) {
            order.setPreparedAt(LocalDateTime.now());
        }

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
        normalizeWalkInOnRequest(request);

        CafeOrder order = cafeOrderRepository.findByOrderCode(code);
        if (order == null) {
            return null;
        }

        OrderStatus previousStatus = order.getStatus();

        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setWalkInGuest(Boolean.TRUE.equals(request.getWalkInGuest()) ? Boolean.TRUE : Boolean.FALSE);

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

        Integer previousTableNumber = order.getTableNumber();
        OrderType previousType = order.getType();

        order.setType(type);

        applyTableAndNoteFromRequest(order, type, request);

        // Đổi bàn / chuyển loại đơn: đồng bộ trạng thái chiếm bàn với Quản lý bàn và kiểm tra đặt bàn.
        if (order.getType() == OrderType.TAKEAWAY) {
            order.setTableReleased(Boolean.FALSE);
        } else if (order.getType() == OrderType.DINE_IN && order.getTableNumber() != null) {
            if (!Objects.equals(previousTableNumber, order.getTableNumber()) || previousType != OrderType.DINE_IN) {
                order.setTableReleased(Boolean.FALSE);
            }
        }

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
        order.setLoyaltyEarnEligible(Boolean.TRUE.equals(request.getWalkInGuest())
                ? Boolean.FALSE
                : computeLoyaltyEarnEligibility(request.getCustomerPhone()));

        // Delete existing items
        List<OrderItem> existingItems = orderItemRepository.findByOrder(order);
        orderItemRepository.deleteAll(existingItems);

        assertValidLoyaltyRedemptions(request);

        BigDecimal subtotal = BigDecimal.ZERO;
        if (request.getItems() != null) {
            for (OrderLineRequest line : request.getItems()) {
                subtotal = subtotal.add(persistOrderLine(order, line));
            }
        }

        order.setSubtotal(subtotal);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(subtotal);

        if (order.getStatus() == OrderStatus.COMPLETED && previousStatus != OrderStatus.COMPLETED) {
            order.setPreparedAt(LocalDateTime.now());
        }

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

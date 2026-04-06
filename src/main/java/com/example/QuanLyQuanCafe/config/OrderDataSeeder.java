package com.example.QuanLyQuanCafe.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.model.OrderItem;
import com.example.QuanLyQuanCafe.model.OrderStatus;
import com.example.QuanLyQuanCafe.model.OrderType;
import com.example.QuanLyQuanCafe.model.PaymentMethod;
import com.example.QuanLyQuanCafe.repository.CafeOrderRepository;
import com.example.QuanLyQuanCafe.repository.MenuItemRepository;
import com.example.QuanLyQuanCafe.repository.OrderItemRepository;
import com.example.QuanLyQuanCafe.service.OrderService;

/**
 * Seed đơn hàng mẫu (idempotent: mã đơn cố định dạng {@code ORD-yymmdd-NNNN} khớp ngày tạo đơn).
 * 28/3–3/4: 5 đơn cố định/ngày + chunk bổ sung từ {@code 0006} (rải giờ, vãng lai / mang đi / VIP tại bàn).
 * 4/4–6/4: chunk đặt từ {@code 0001}. Trạng thái {@link OrderStatus#COMPLETED}.
 * <p>Cờ tích điểm khớp 3 case: vãng lai → {@code walkInGuest=true}, không tích điểm;
 * có SĐT → {@code loyaltyEarnEligible=true}, {@code loyaltyPointsAwarded=false} rồi gọi
 * {@link OrderService#applyLoyaltyAndCustomerStatsForSeededCompletedOrder} để cộng điểm + cập nhật khách (giống runtime).
 */
@Component
public class OrderDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(OrderDataSeeder.class);

    private final CafeOrderRepository cafeOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderService orderService;

    public OrderDataSeeder(
            CafeOrderRepository cafeOrderRepository,
            OrderItemRepository orderItemRepository,
            MenuItemRepository menuItemRepository,
            OrderService orderService) {
        this.cafeOrderRepository = cafeOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderService = orderService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(200)
    @Transactional
    public void seedOrders() {
        if (menuItemRepository.count() == 0) {
            log.warn("OrderDataSeeder: bỏ qua — chưa có món trong menu.");
            return;
        }

        seedOrdersMar28ThroughApr3_2026();
        seedAdditionalOrdersMar28ThroughApr6_2026();
    }

    /**
     * Đơn mẫu 28/3–3/4/2026, giờ trong [7h, 21h], ~5 đơn/ngày, trạng thái hoàn thành.
     * Mã {@code ORD-yymmdd-0001} … {@code -0005} theo từng ngày — idempotent.
     */
    private void seedOrdersMar28ThroughApr3_2026() {
        List<RangeDayOrder> mar28 = List.of(
                new RangeDayOrder("ORD-260328-0001", 7, 18, true, 2, "Dương Thế Vinh", "0906280101",
                        null, PaymentMethod.CASH,
                        new String[][] { { "Espresso", "1", null }, { "Sandwich gà", "1", null } }),
                new RangeDayOrder("ORD-260328-0002", 10, 22, false, null, "Nguyễn Thị Lam", "0906280102",
                        "Mang đi", PaymentMethod.CARD,
                        new String[][] { { "Trà sữa truyền thống", "2", "Ít đá" } }),
                new RangeDayOrder("ORD-260328-0003", 13, 40, true, 8, "Hoàng Anh Tuấn", "0906280103",
                        null, PaymentMethod.BANK_TRANSFER,
                        new String[][] { { "Latte", "1", null }, { "Sandwich trứng", "1", null } }),
                new RangeDayOrder("ORD-260328-0004", 16, 55, false, null, "Lê Khánh Ngọc", "0906280104",
                        null, PaymentMethod.CASH,
                        new String[][] { { "Soda chanh", "1", null }, { "Khoai tây chiên", "1", null } }),
                new RangeDayOrder("ORD-260328-0005", 20, 5, true, 13, null, null,
                        "Khách vãng lai", PaymentMethod.CASH,
                        new String[][] { { "Bạc xỉu", "2", null } }));

        List<RangeDayOrder> mar29 = List.of(
                new RangeDayOrder("ORD-260329-0001", 7, 45, false, null, "Phạm Gia Bảo", "0906290101",
                        "Sáng thứ bảy", PaymentMethod.CASH,
                        new String[][] { { "Americano", "2", null } }),
                new RangeDayOrder("ORD-260329-0002", 11, 5, true, 4, "Vũ Thùy Dương", "0906290102",
                        null, PaymentMethod.CARD,
                        new String[][] { { "Cappuccino", "1", null }, { "Pain au chocolat", "1", null } }),
                new RangeDayOrder("ORD-260329-0003", 14, 28, false, null, "Đặng Minh Quân", "0906290103",
                        "Lấy ngay", PaymentMethod.CASH,
                        new String[][] { { "Trà chanh mật ong", "1", null }, { "Brownie", "1", null } }),
                new RangeDayOrder("ORD-260329-0004", 17, 42, true, 16, "Trương Nhã Uyên", "0906290104",
                        "Họp nhóm", PaymentMethod.BANK_TRANSFER,
                        new String[][] { { "Cold Brew", "3", null } }),
                new RangeDayOrder("ORD-260329-0005", 20, 18, true, 6, "Bùi Sơn Tùng", "0906290105",
                        null, PaymentMethod.CASH,
                        new String[][] { { "Mocha", "1", null }, { "Cheesecake", "1", null } }));

        List<RangeDayOrder> mar30 = List.of(
                new RangeDayOrder("ORD-260330-0001", 7, 30, true, 1, "Đỗ Quỳnh Chi", "0906300101",
                        "Gần quầy bar", PaymentMethod.CARD,
                        new String[][] { { "Cà phê sữa đá", "1", null } }),
                new RangeDayOrder("ORD-260330-0002", 10, 48, false, null, "Hồ Nam Phong", "0906300102",
                        null, PaymentMethod.CASH,
                        new String[][] { { "Matcha Latte", "1", null }, { "Croissant bơ", "1", null } }),
                new RangeDayOrder("ORD-260330-0003", 13, 12, true, 11, "Lý Bảo Ngân", "0906300103",
                        null, PaymentMethod.CASH,
                        new String[][] { { "Trà đào cam sả", "2", null } }),
                new RangeDayOrder("ORD-260330-0004", 17, 8, false, null, "Châu Bảo Long", "0906300104",
                        "Không nhận marketing", PaymentMethod.BANK_TRANSFER,
                        new String[][] { { "Chocolate đá", "1", null }, { "Tiramisu", "1", null } }),
                new RangeDayOrder("ORD-260330-0005", 20, 48, true, 19, null, null,
                        null, PaymentMethod.CARD,
                        new String[][] { { "Sinh tố bơ", "1", null }, { "Hạt mix", "1", null } }));

        List<RangeDayOrder> mar31 = List.of(
                new RangeDayOrder("ORD-260331-0001", 7, 12, true, 4, "Ngô Thảo Vy", "0907001001",
                        null, PaymentMethod.CASH,
                        new String[][] { { "Espresso", "1", null }, { "Croissant bơ", "1", null } }),
                new RangeDayOrder("ORD-260331-0002", 10, 35, false, null, "Trịnh Bảo Long", "0907001002",
                        "Mang đi — nóng", PaymentMethod.CARD,
                        new String[][] { { "Latte", "2", null } }),
                new RangeDayOrder("ORD-260331-0003", 13, 48, true, 9, "Lý Minh Tuấn", "0907001003",
                        null, PaymentMethod.BANK_TRANSFER,
                        new String[][] { { "Trà đào cam sả", "2", null }, { "Sandwich gà", "1", null } }),
                new RangeDayOrder("ORD-260331-0004", 17, 5, false, null, "Chu Diệu Linh", "0907001004",
                        "Lấy sau 15 phút", PaymentMethod.CASH,
                        new String[][] { { "Cold Brew", "1", null }, { "Brownie", "2", null } }),
                new RangeDayOrder("ORD-260331-0005", 20, 22, true, 14, null, null,
                        "Khách ngồi lâu", PaymentMethod.CARD,
                        new String[][] { { "Mocha", "1", null }, { "Cheesecake", "1", null } }));

        List<RangeDayOrder> apr1 = List.of(
                new RangeDayOrder("ORD-260401-0001", 7, 40, false, null, "Hồ Ngọc Sơn", "0908002001",
                        "Sáng sớm", PaymentMethod.CASH,
                        new String[][] { { "Americano", "2", null } }),
                new RangeDayOrder("ORD-260401-0002", 11, 8, true, 6, "Đinh Khánh Ly", "0908002002",
                        null, PaymentMethod.CASH,
                        new String[][] { { "Cappuccino", "1", null }, { "Pain au chocolat", "1", null } }),
                new RangeDayOrder("ORD-260401-0003", 14, 25, false, null, "Mai Phương Anh", "0908002003",
                        null, PaymentMethod.BANK_TRANSFER,
                        new String[][] { { "Matcha Latte", "1", "Ít ngọt" }, { "Sinh tố xoài", "1", null } }),
                new RangeDayOrder("ORD-260401-0004", 18, 50, true, 11, "Vũ Hoàng Nam", "0908002004",
                        "Cuối giờ", PaymentMethod.CARD,
                        new String[][] { { "Bạc xỉu", "2", null } }),
                new RangeDayOrder("ORD-260401-0005", 20, 40, false, null, "Phan Thu Trang", "0908002005",
                        "Order cuối ngày", PaymentMethod.CASH,
                        new String[][] { { "Chocolate nóng", "1", null }, { "Tiramisu", "1", null } }));

        List<RangeDayOrder> apr2 = List.of(
                new RangeDayOrder("ORD-260402-0001", 7, 25, false, null, "Cao Thị Yến", "0909003001",
                        "Mang đi nhanh", PaymentMethod.CARD,
                        new String[][] { { "Cà phê trứng", "1", null } }),
                new RangeDayOrder("ORD-260402-0002", 10, 50, true, 3, "Lương Đức Thắng", "0909003002",
                        null, PaymentMethod.CASH,
                        new String[][] { { "Trà vải", "2", null }, { "Khoai tây chiên", "1", null } }),
                new RangeDayOrder("ORD-260402-0003", 14, 2, false, null, "Tôn Nữ Minh Châu", "0909003003",
                        null, PaymentMethod.BANK_TRANSFER,
                        new String[][] { { "Cold Brew cam", "1", null }, { "Muffin việt quất", "1", null } }),
                new RangeDayOrder("ORD-260402-0004", 17, 35, true, 10, "Quách Hải Đăng", "0909003004",
                        "Gần ổ cắm", PaymentMethod.CASH,
                        new String[][] { { "Mocha Frappe", "1", null } }),
                new RangeDayOrder("ORD-260402-0005", 20, 8, true, 8, null, null,
                        "Khách ghé muộn", PaymentMethod.CARD,
                        new String[][] { { "Americano", "1", null }, { "Sandwich trứng", "1", null } }));

        List<RangeDayOrder> apr3 = List.of(
                new RangeDayOrder("ORD-260403-0001", 7, 50, true, 5, "Kiều Anh Thư", "0910004001",
                        "Sáng thứ 5", PaymentMethod.CASH,
                        new String[][] { { "Cappuccino", "1", null }, { "Croissant bơ", "1", null } }),
                new RangeDayOrder("ORD-260403-0002", 11, 22, false, null, "Bạch Dương", "0910004002",
                        null, PaymentMethod.CARD,
                        new String[][] { { "Nước ép cam", "2", null } }),
                new RangeDayOrder("ORD-260403-0003", 13, 55, true, 15, "Hà Kiều My", "0910004003",
                        null, PaymentMethod.CASH,
                        new String[][] { { "Matcha Cloud", "1", null }, { "Salad ức gà", "1", null } }),
                new RangeDayOrder("ORD-260403-0004", 17, 18, false, null, "Giáp Tuấn Kiệt", "0910004004",
                        "Không ống hút", PaymentMethod.BANK_TRANSFER,
                        new String[][] { { "Soda đào", "1", null }, { "Gà nuggets", "1", null } }),
                new RangeDayOrder("ORD-260403-0005", 20, 35, true, 1, "La Ngọc Hân", "0910004005",
                        "Cuối ca", PaymentMethod.CASH,
                        new String[][] { { "Chocolate đá", "1", null }, { "Brownie", "1", null } }));

        for (RangeDayOrder cfg : mar28) {
            applyRangeDayOrder(2026, 3, 28, cfg);
        }
        for (RangeDayOrder cfg : mar29) {
            applyRangeDayOrder(2026, 3, 29, cfg);
        }
        for (RangeDayOrder cfg : mar30) {
            applyRangeDayOrder(2026, 3, 30, cfg);
        }
        for (RangeDayOrder cfg : mar31) {
            applyRangeDayOrder(2026, 3, 31, cfg);
        }
        for (RangeDayOrder cfg : apr1) {
            applyRangeDayOrder(2026, 4, 1, cfg);
        }
        for (RangeDayOrder cfg : apr2) {
            applyRangeDayOrder(2026, 4, 2, cfg);
        }
        for (RangeDayOrder cfg : apr3) {
            applyRangeDayOrder(2026, 4, 3, cfg);
        }
    }

    private void applyRangeDayOrder(int year, int month, int day, RangeDayOrder cfg) {
        LocalDateTime created = LocalDateTime.of(year, month, day, cfg.hour, cfg.minute);
        LocalDateTime prepared = created.plusMinutes(11);
        LocalDateTime paid = prepared.plusMinutes(7);

        if (cfg.walkInDineIn) {
            if (cafeOrderRepository.findByOrderCode(cfg.orderCode) != null) {
                return;
            }
            seedOrderDineInWalkInAt(cfg.orderCode, created, prepared, paid, cfg.tableNumber, cfg.orderNote,
                    cfg.payment, cfg.lines);
            return;
        }

        if (cfg.tableNumber != null) {
            seedOrderDineIn(
                    cfg.orderCode,
                    created,
                    prepared,
                    paid,
                    nvl(cfg.customerName, "Khách"),
                    cfg.customerPhone,
                    cfg.tableNumber,
                    cfg.orderNote,
                    cfg.payment,
                    cfg.lines);
        } else {
            seedOrderTakeaway(
                    cfg.orderCode,
                    created,
                    prepared,
                    paid,
                    nvl(cfg.customerName, "Khách"),
                    nvl(cfg.customerPhone, "0900000999"),
                    nvl(cfg.orderNote, "Mang về"),
                    cfg.payment,
                    cfg.lines);
        }
    }

    private void seedOrderDineInWalkInAt(
            String orderCode,
            LocalDateTime createdAt,
            LocalDateTime preparedAt,
            LocalDateTime paidAt,
            int tableNumber,
            String orderNote,
            PaymentMethod payment,
            String[][] lines) {
        if (cafeOrderRepository.findByOrderCode(orderCode) != null) {
            return;
        }
        CafeOrder order = baseCompletedOrder(orderCode, createdAt, preparedAt, paidAt, "Khách vãng lai", null);
        order.setWalkInGuest(Boolean.TRUE);
        order.setLoyaltyEarnEligible(Boolean.FALSE);
        order.setLoyaltyPointsAwarded(Boolean.TRUE);
        order.setType(OrderType.DINE_IN);
        order.setTableNumber(tableNumber);
        order.setOrderNote(orderNote);
        order.setPaymentMethod(payment);
        order.setTableReleased(Boolean.TRUE);
        cafeOrderRepository.save(order);
        BigDecimal total = appendLines(order, lines);
        finalizeOrderTotals(order, total);
    }

    private static String nvl(String s, String fallback) {
        return (s != null && !s.isBlank()) ? s : fallback;
    }

    private static final class RangeDayOrder {
        final String orderCode;
        final int hour;
        final int minute;
        /** {@code true} = tại bàn khách vãng lai (không SĐT). */
        final boolean walkInDineIn;
        final Integer tableNumber;
        final String customerName;
        final String customerPhone;
        final String orderNote;
        final PaymentMethod payment;
        final String[][] lines;

        RangeDayOrder(
                String orderCode,
                int hour,
                int minute,
                boolean dineInAtTable,
                Integer tableNumber,
                String customerName,
                String customerPhone,
                String orderNote,
                PaymentMethod payment,
                String[][] lines) {
            this.orderCode = orderCode;
            this.hour = hour;
            this.minute = minute;
            this.walkInDineIn = dineInAtTable && customerPhone == null;
            this.tableNumber = tableNumber;
            this.customerName = customerName;
            this.customerPhone = customerPhone;
            this.orderNote = orderNote;
            this.payment = payment;
            this.lines = lines;
        }
    }

    private void seedOrderDineIn(
            String orderCode,
            LocalDateTime createdAt,
            LocalDateTime preparedAt,
            LocalDateTime paidAt,
            String customerName,
            String customerPhone,
            int tableNumber,
            String orderNote,
            PaymentMethod payment,
            String[][] lines) {
        if (cafeOrderRepository.findByOrderCode(orderCode) != null) {
            return;
        }
        CafeOrder order = baseCompletedOrder(orderCode, createdAt, preparedAt, paidAt, customerName, customerPhone);
        order.setType(OrderType.DINE_IN);
        order.setTableNumber(tableNumber);
        order.setOrderNote(orderNote);
        order.setPaymentMethod(payment);
        order.setTableReleased(Boolean.TRUE);
        cafeOrderRepository.save(order);
        BigDecimal total = appendLines(order, lines);
        finalizeOrderTotals(order, total);
        orderService.applyLoyaltyAndCustomerStatsForSeededCompletedOrder(orderCode);
    }

    private void seedOrderTakeaway(
            String orderCode,
            LocalDateTime createdAt,
            LocalDateTime preparedAt,
            LocalDateTime paidAt,
            String customerName,
            String customerPhone,
            String orderNote,
            PaymentMethod payment,
            String[][] lines) {
        if (cafeOrderRepository.findByOrderCode(orderCode) != null) {
            return;
        }
        CafeOrder order = baseCompletedOrder(orderCode, createdAt, preparedAt, paidAt, customerName, customerPhone);
        order.setType(OrderType.TAKEAWAY);
        order.setTableNumber(null);
        order.setOrderNote(orderNote);
        order.setPaymentMethod(payment);
        order.setTableReleased(Boolean.FALSE);
        cafeOrderRepository.save(order);
        BigDecimal total = appendLines(order, lines);
        finalizeOrderTotals(order, total);
        orderService.applyLoyaltyAndCustomerStatsForSeededCompletedOrder(orderCode);
    }

    private static CafeOrder baseCompletedOrder(
            String orderCode,
            LocalDateTime createdAt,
            LocalDateTime preparedAt,
            LocalDateTime paidAt,
            String customerName,
            String customerPhone) {
        CafeOrder order = new CafeOrder();
        order.setOrderCode(orderCode);
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);
        order.setCustomer(null);
        order.setStatus(OrderStatus.COMPLETED);
        order.setCreatedAt(createdAt);
        order.setPreparedAt(preparedAt);
        order.setPaidAt(paidAt);
        order.setDiscount(BigDecimal.ZERO);
        // Case 1 (không SĐT): không tích. Case 2–3: có SĐT → đủ điều kiện; mã ORD-yymmdd-NNNN; cộng điểm qua OrderService sau khi seed dòng món.
        boolean hasPhone = customerPhone != null && !customerPhone.isBlank();
        order.setLoyaltyEarnEligible(hasPhone ? Boolean.TRUE : Boolean.FALSE);
        order.setLoyaltyPointsAwarded(hasPhone ? Boolean.FALSE : Boolean.TRUE);
        order.setWalkInGuest(Boolean.FALSE);
        order.setCreatedBy(null);
        return order;
    }

    private BigDecimal appendLines(CafeOrder order, String[][] lines) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (String[] line : lines) {
            if (line == null || line.length < 2) {
                continue;
            }
            String itemName = line[0];
            int qty = parsePositiveInt(line[1], 1);
            String note = line.length > 2 ? line[2] : null;
            Optional<MenuItem> opt = menuItemRepository.findByName(itemName);
            if (opt.isEmpty()) {
                log.warn("OrderDataSeeder: bỏ qua dòng món '{}' — không có trong menu.", itemName);
                continue;
            }
            MenuItem menuItem = opt.get();
            BigDecimal unit = menuItem.getPrice() != null ? menuItem.getPrice() : BigDecimal.ZERO;
            BigDecimal cost = menuItem.getCost() != null ? menuItem.getCost() : BigDecimal.ZERO;
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(qty));

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setMenuItem(menuItem);
            oi.setItemName(menuItem.getName());
            oi.setItemPrice(unit);
            oi.setItemCost(cost);
            oi.setQuantity(qty);
            oi.setSubtotal(lineTotal);
            oi.setNote(note);
            oi.setLoyaltyRedemption(false);
            orderItemRepository.save(oi);
            subtotal = subtotal.add(lineTotal);
        }
        return subtotal;
    }

    private void finalizeOrderTotals(CafeOrder order, BigDecimal subtotal) {
        order.setSubtotal(subtotal);
        order.setTotal(subtotal);
        cafeOrderRepository.save(order);
    }

    private static int parsePositiveInt(String raw, int defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            return v > 0 ? v : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Seed bổ sung (sau 5 đơn cố định/ngày 28/3–3/4 nếu có):
     * - 28/3–3/4: thêm chunk từ mã {@code 0006}, mỗi ngày tổng ~21–27 đơn (5 gốc + chunk), số chunk khác nhau;
     *   giờ rải + mix vãng lai / mang đi / VIP tại bàn (tích điểm).
     * - 4/4–6/4: chỉ chunk (mã từ {@code 0001}), 20–30 đơn/ngày, cùng kiểu rải.
     * - Mã {@code ORD-yymmdd-NNNN}, idempotent qua {@code findByOrderCode}.
     */
    private void seedAdditionalOrdersMar28ThroughApr6_2026() {
        seedSpreadOrdersChunk(LocalDate.of(2026, 3, 28), 6, 19, 3);
        seedSpreadOrdersChunk(LocalDate.of(2026, 3, 29), 6, 17, 2);
        seedSpreadOrdersChunk(LocalDate.of(2026, 3, 30), 6, 21, 3);
        seedSpreadOrdersChunk(LocalDate.of(2026, 3, 31), 6, 18, 2);
        seedSpreadOrdersChunk(LocalDate.of(2026, 4, 1), 6, 19, 3);
        seedSpreadOrdersChunk(LocalDate.of(2026, 4, 2), 6, 22, 3);
        seedSpreadOrdersChunk(LocalDate.of(2026, 4, 3), 6, 16, 2);

        seedSpreadOrdersChunk(LocalDate.of(2026, 4, 4), 1, 26, 3);
        seedSpreadOrdersChunk(LocalDate.of(2026, 4, 5), 1, 22, 2);
        seedSpreadOrdersChunk(LocalDate.of(2026, 4, 6), 1, 30, 3);
    }

    /**
     * Một dải đơn cùng ngày: {@code count} đơn với STT {@code startSeq} … {@code startSeq+count-1}.
     * Salt phụ thuộc ngày + {@code startSeq} để chunk sau 0005 khác hẳn chunk chỉ có mang đi/ngày 4–6/4.
     */
    private void seedSpreadOrdersChunk(LocalDate day, int startSeq, int count, int loyaltyDineInCount) {
        if (count <= 0) {
            return;
        }
        long salt = day.toEpochDay() * 31L + startSeq * 4099L;
        Set<Integer> loyaltyDineIn = loyaltyDineInIndices(count, loyaltyDineInCount, salt);

        for (int i = 0; i < count; i++) {
            int seq = startSeq + i;
            String orderCode = String.format("ORD-%02d%02d%02d-%04d",
                    day.getYear() % 100, day.getMonthValue(), day.getDayOfMonth(), seq);

            int minuteOfDay = spreadMinuteOfDay(day, count, i, salt);
            int hour = minuteOfDay / 60;
            int minute = minuteOfDay % 60;
            LocalDateTime created = day.atTime(hour, minute);
            int prepDelta = 8 + Math.floorMod((int) (salt + i * 19), 9);
            int payDelta = 4 + Math.floorMod((int) (salt * 3 + i * 11), 8);
            LocalDateTime prepared = created.plusMinutes(prepDelta);
            LocalDateTime paid = prepared.plusMinutes(payDelta);

            PaymentMethod payment = switch (Math.floorMod((int) salt + i, 3)) {
                case 0 -> PaymentMethod.CASH;
                case 1 -> PaymentMethod.CARD;
                default -> PaymentMethod.BANK_TRANSFER;
            };
            String[][] lines = generatedOrderLines(i, salt);

            if (loyaltyDineIn.contains(i)) {
                String customerName = "Khách VIP " + day.getDayOfMonth() + "/" + day.getMonthValue() + " #" + seq;
                String customerPhone = String.format("098%02d%02d%04d",
                        day.getMonthValue(), day.getDayOfMonth(), seq);
                int tableNo = 1 + Math.floorMod((int) (salt * 5 + i * 7), 20);
                seedOrderDineIn(orderCode, created, prepared, paid, customerName, customerPhone, tableNo,
                        "Tích điểm — tại bàn", payment, lines);
            } else {
                int r = Math.floorMod((int) (salt / 13) + i * 23, 10);
                if (r < 3) {
                    String customerName = "Khách mang về " + seq;
                    String customerPhone = String.format("096%02d%02d%04d",
                            day.getMonthValue(), day.getDayOfMonth(), seq);
                    seedOrderTakeaway(orderCode, created, prepared, paid, customerName, customerPhone,
                            "Mang đi — " + Math.floorMod(i, 5), payment, lines);
                } else {
                    int tableNo = 1 + Math.floorMod((int) (salt + i * 13), 20);
                    String note = switch (Math.floorMod(i, 4)) {
                        case 0 -> "Khách vãng lai";
                        case 1 -> "Vãng lai — gần cửa";
                        case 2 -> "Walk-in ca " + (hour < 12 ? "sáng" : hour < 17 ? "trưa" : "chiều");
                        default -> "Ghé nhanh";
                    };
                    seedOrderDineInWalkInAt(orderCode, created, prepared, paid, tableNo, note, payment, lines);
                }
            }
        }
    }

    /** Rải giờ trong khoảng 7:00–21:45, tránh trùng nhau quá nhiều giữa các ngày. */
    private static int spreadMinuteOfDay(LocalDate day, int count, int index, long salt) {
        int start = 7 * 60;
        int end = 21 * 60 + 45;
        int span = end - start;
        if (count <= 0) {
            return start;
        }
        int base = start + (index * span) / count;
        int jitter = Math.floorMod((int) (salt * 97 + index * 503 + day.getDayOfMonth() * 17), Math.max(span / (count + 2), 12));
        int zig = Math.floorMod(index * index * 7, 25);
        int t = base + jitter + zig - 12;
        return Math.min(Math.max(t, start), end);
    }

    private static Set<Integer> loyaltyDineInIndices(int count, int loyaltyCount, long salt) {
        Set<Integer> set = new HashSet<>();
        if (count <= 0 || loyaltyCount <= 0) {
            return set;
        }
        int want = Math.min(loyaltyCount, count);
        int step = Math.max(1, count / (want + 2));
        int seed = Math.floorMod((int) salt, count);
        for (int k = 0; k < want; k++) {
            int idx = Math.floorMod(seed + k * step * 19 + k * k * 3, count);
            int guard = 0;
            while (set.contains(idx) && guard < count) {
                idx = Math.floorMod(idx + 7, count);
                guard++;
            }
            set.add(idx);
        }
        return set;
    }

    private String[][] generatedOrderLines(int idx, long salt) {
        int p = Math.floorMod(idx * 3 + (int) (salt % 211), 8);
        String[][] pattern0 = {
                { "Espresso", "1", null },
                { "Croissant bơ", "1", null }
        };
        String[][] pattern1 = {
                { "Latte", "1", null },
                { "Brownie", "1", null }
        };
        String[][] pattern2 = {
                { "Americano", "2", null }
        };
        String[][] pattern3 = {
                { "Matcha Latte", "1", "Ít ngọt" },
                { "Khoai tây chiên", "1", null }
        };
        String[][] pattern4 = {
                { "Cold Brew", "1", null },
                { "Cheesecake", "1", null }
        };
        String[][] pattern5 = {
                { "Trà vải", "1", null },
                { "Muffin việt quất", "1", null }
        };
        String[][] pattern6 = {
                { "Cappuccino", "1", null },
                { "Pain au chocolat", "1", null }
        };
        String[][] pattern7 = {
                { "Trà đào cam sả", "1", null },
                { "Sandwich gà", "1", null }
        };
        return switch (p) {
            case 0 -> pattern0;
            case 1 -> pattern1;
            case 2 -> pattern2;
            case 3 -> pattern3;
            case 4 -> pattern4;
            case 5 -> pattern5;
            case 6 -> pattern6;
            default -> pattern7;
        };
    }
}

package com.example.QuanLyQuanCafe.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.QuanLyQuanCafe.model.BookingStatus;
import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.model.OrderItem;
import com.example.QuanLyQuanCafe.model.OrderStatus;
import com.example.QuanLyQuanCafe.model.OrderType;
import com.example.QuanLyQuanCafe.model.PaymentMethod;
import com.example.QuanLyQuanCafe.model.TableBooking;
import com.example.QuanLyQuanCafe.repository.CafeOrderRepository;
import com.example.QuanLyQuanCafe.repository.MenuItemRepository;
import com.example.QuanLyQuanCafe.repository.OrderItemRepository;
import com.example.QuanLyQuanCafe.repository.TableBookingRepository;
import com.example.QuanLyQuanCafe.service.OrderService;

/**
 * Seed đơn hàng và đặt bàn mẫu (idempotent: mã đơn cố định dạng {@code ORD-yymmdd-NNNN} khớp ngày tạo đơn; booking theo cặp SĐT + giờ đặt).
 * Đơn khoảng 28/3–3/4; lịch đặt bàn dày 28/3–3/4 (mỗi {@code booking_time} duy nhất toàn DB).
 * Trạng thái {@link OrderStatus#COMPLETED} / {@link BookingStatus#CHECKED_IN} cho dữ liệu lịch sử đặt bàn.
 * <p>Cờ tích điểm khớp 3 case: vãng lai → {@code walkInGuest=true}, không tích điểm;
 * có SĐT → {@code loyaltyEarnEligible=true}, {@code loyaltyPointsAwarded=false} rồi gọi
 * {@link OrderService#applyLoyaltyAndCustomerStatsForSeededCompletedOrder} để cộng điểm + cập nhật khách (giống runtime).
 */
@Component
public class OrderAndBookingDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(OrderAndBookingDataSeeder.class);

    private final CafeOrderRepository cafeOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final TableBookingRepository tableBookingRepository;
    private final OrderService orderService;

    public OrderAndBookingDataSeeder(
            CafeOrderRepository cafeOrderRepository,
            OrderItemRepository orderItemRepository,
            MenuItemRepository menuItemRepository,
            TableBookingRepository tableBookingRepository,
            OrderService orderService) {
        this.cafeOrderRepository = cafeOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.tableBookingRepository = tableBookingRepository;
        this.orderService = orderService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(200)
    @Transactional
    public void seedOrdersAndBookings() {
        if (menuItemRepository.count() == 0) {
            log.warn("OrderAndBookingDataSeeder: bỏ qua — chưa có món trong menu.");
            return;
        }

        seedOrdersMar28ThroughApr3_2026();
        seedDemoBookings();
        seedBookingsMar28ThroughApr3_2026();
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
                log.warn("OrderAndBookingDataSeeder: bỏ qua dòng món '{}' — không có trong menu.", itemName);
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

    private void seedDemoBookings() {
        upsertCompletedBooking(
                "Nguyễn Văn An",
                "0912000101",
                "an.nguyen@demo.local",
                LocalDateTime.of(2026, 3, 8, 18, 0),
                LocalDateTime.of(2026, 3, 1, 10, 0),
                4,
                3,
                "Sinh nhật — dễ uống ngọt nhẹ");

        upsertCompletedBooking(
                "Hoàng Thị Mai",
                "0912000102",
                "mai.hoang@demo.local",
                LocalDateTime.of(2026, 3, 12, 12, 0),
                LocalDateTime.of(2026, 3, 5, 9, 0),
                2,
                5,
                "Gần cửa sổ");

        upsertCompletedBooking(
                "Võ Đức Thịnh",
                "0912000103",
                null,
                LocalDateTime.of(2026, 3, 16, 19, 0),
                LocalDateTime.of(2026, 3, 10, 14, 0),
                6,
                8,
                "Có trẻ em — ghế thêm nếu có");

        upsertCompletedBooking(
                "Bùi Lan Chi",
                "0912000104",
                "chi.bui@demo.local",
                LocalDateTime.of(2026, 3, 20, 15, 0),
                LocalDateTime.of(2026, 3, 12, 11, 0),
                3,
                2,
                "Họp nhóm nhỏ");
    }

    /**
     * Lịch đặt bàn 28/3–3/4/2026: 5 slot/ngày × 7 ngày = 35 bản ghi.
     * Giờ đặt giờ chẵn (phút 0), trong mỗi ngày cách nhau đúng 1 giờ; mỗi {@code booking_time} duy nhất toàn DB.
     * SĐT 0915055001–0915055035; idempotent: bỏ qua nếu trùng giờ đặt hoặc trùng cặp SĐT + giờ.
     */
    private void seedBookingsMar28ThroughApr3_2026() {
        int seq = 1;
        seq = seedCalendarBookings(2026, 3, 28, seq, List.of(
                new CalSlot(10, 0, 2, 5, "Trần Thu Hương", "huong.tran@demo.local", "Uống trà chiều"),
                new CalSlot(11, 0, 4, 7, "Lê Quốc Huy", "huy.le@demo.local", "Sinh nhật bạn"),
                new CalSlot(12, 0, 3, null, "Phạm Ngọc Lan", null, "Chưa chọn bàn — linh hoạt"),
                new CalSlot(13, 0, 8, 12, "Hoàng Đức Anh", "anh.hoang@demo.local", "Họp nhóm lớn"),
                new CalSlot(14, 0, 2, 3, "Võ Thị Mai", null, "Cuối ngày, yên tĩnh")));
        seq = seedCalendarBookings(2026, 3, 29, seq, List.of(
                new CalSlot(11, 0, 3, 4, "Đặng Minh Khôi", "khoi.dang@demo.local", "Brunch cuối tuần"),
                new CalSlot(12, 0, 2, 6, "Bùi Thảo My", null, "Gần cửa sổ"),
                new CalSlot(13, 0, 5, 9, "Nguyễn Hải Nam", "nam.nguyen@demo.local", "Có trẻ nhỏ"),
                new CalSlot(14, 0, 6, 11, "Đinh Thuỳ Linh", "linh.dinh@demo.local", "Tiệc nhỏ"),
                new CalSlot(15, 0, 4, 8, "Mai Phương Đông", null, "Tối thứ bảy")));
        seq = seedCalendarBookings(2026, 3, 30, seq, List.of(
                new CalSlot(9, 0, 2, 2, "Lý Gia Hân", "han.ly@demo.local", "Sáng sớm"),
                new CalSlot(10, 0, 3, 10, "Chu Bảo Long", null, "Trưa vắng"),
                new CalSlot(11, 0, 7, 14, "Tôn Nữ Ánh Tuyết", "tuyet.book@demo.local", "Họp team"),
                new CalSlot(12, 0, 2, 5, "Cao Hoài Nam", null, "Hẹn gặp bạn"),
                new CalSlot(13, 0, 4, 16, "Kiều Bích Ngọc", "ngoc.kieu@demo.local", "Tối xem bóng đá")));
        seq = seedCalendarBookings(2026, 3, 31, seq, List.of(
                new CalSlot(12, 0, 3, 6, "Quách Đình Phúc", "phuc.quach@demo.local", "Cuối tháng"),
                new CalSlot(13, 0, 5, 13, "Hà Thu Trang", null, "Họp phụ huynh xong"),
                new CalSlot(14, 0, 2, 1, "La Tuấn Kiệt", "kiet.la@demo.local", null),
                new CalSlot(15, 0, 8, 18, "Giáp Thị Yến", null, "Tiệc chia tay đồng nghiệp"),
                new CalSlot(16, 0, 3, 4, "Phan Bảo Châu", "chau.phan@demo.local", "Slot muộn")));
        seq = seedCalendarBookings(2026, 4, 1, seq, List.of(
                new CalSlot(10, 0, 2, 3, "Vương Thế Sơn", "son.vuong@demo.local", "Cà phê sáng 1/4"),
                new CalSlot(11, 0, 6, 15, "Thân Minh Tuấn", null, "Đông người"),
                new CalSlot(12, 0, 4, 7, "Uông Thị Hạnh", "hanh.uong@demo.local", "Chiều mát"),
                new CalSlot(13, 0, 2, 2, "Dương Kim Ngân", null, "Đi một mình"),
                new CalSlot(14, 0, 5, 17, "Từ Đức Thịnh", "thinh.tu@demo.local", "Tối muộn")));
        seq = seedCalendarBookings(2026, 4, 2, seq, List.of(
                new CalSlot(11, 0, 3, 8, "Hồ Ngọc Bích", null, "Thử cold brew"),
                new CalSlot(12, 0, 4, 9, "Lương Văn Tài", "tai.luong@demo.local", "Họp dự án"),
                new CalSlot(13, 0, 2, 5, "Tạ Minh Tuệ", null, "Góc làm việc"),
                new CalSlot(14, 0, 7, 12, "Đỗ Quang Huy", "huy.do@demo.local", "Nhóm bạn đông"),
                new CalSlot(15, 0, 3, 6, "Âu Dương Phong", null, "Sau xem phim")));
        seedCalendarBookings(2026, 4, 3, seq, List.of(
                new CalSlot(10, 0, 4, 10, "Khúc Anh Thư", "thu.khuc@demo.local", "Trưa Chủ nhật"),
                new CalSlot(11, 0, 2, 3, "Tiêu Việt Hùng", null, "Thư giãn"),
                new CalSlot(12, 0, 6, 11, "Chế Linh Phụng", "phung.che@demo.local", "Sinh nhật con"),
                new CalSlot(13, 0, 3, 14, "Viên Hoài Thương", null, "Gặp bạn cũ"),
                new CalSlot(14, 0, 5, 19, "Tô Hiếu Nghĩa", "nghia.to@demo.local", "Khách quen — bàn quen")));
    }

    private int seedCalendarBookings(int year, int month, int day, int phoneSeq, List<CalSlot> slots) {
        int n = phoneSeq;
        for (CalSlot s : slots) {
            LocalDateTime bookingTime = LocalDateTime.of(year, month, day, s.hour(), s.minute());
            LocalDateTime createdAt = bookingTime.minusDays(1).withHour(9).withMinute(0);
            if (!createdAt.isBefore(bookingTime)) {
                createdAt = bookingTime.minusHours(3).withMinute(0);
            }
            String phone = String.format("0915055%03d", n);
            upsertCompletedBooking(s.name(), phone, s.email(), bookingTime, createdAt, s.guests(), s.table(), s.note());
            n++;
        }
        return n;
    }

    private record CalSlot(int hour, int minute, int guests, Integer table, String name, String email, String note) {
    }

    private void upsertCompletedBooking(
            String name,
            String phone,
            String email,
            LocalDateTime bookingTime,
            LocalDateTime createdAt,
            int guests,
            Integer reservedTable,
            String note) {
        if (tableBookingRepository.existsByBookingTime(bookingTime)) {
            return;
        }
        if (tableBookingRepository.existsByPhoneAndBookingTime(phone, bookingTime)) {
            return;
        }
        TableBooking b = new TableBooking();
        b.setName(name);
        b.setPhone(phone);
        b.setEmail(email);
        b.setBookingTime(bookingTime);
        b.setGuests(guests);
        b.setNote(note);
        b.setCreatedAt(createdAt);
        b.setStatus(BookingStatus.CHECKED_IN);
        b.setReservedTableNumber(reservedTable);
        tableBookingRepository.save(b);
    }
}

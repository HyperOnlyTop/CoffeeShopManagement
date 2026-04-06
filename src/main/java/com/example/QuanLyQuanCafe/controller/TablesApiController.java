package com.example.QuanLyQuanCafe.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.model.BookingStatus;
import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.OrderItem;
import com.example.QuanLyQuanCafe.model.OrderStatus;
import com.example.QuanLyQuanCafe.model.OrderType;
import com.example.QuanLyQuanCafe.model.TableBooking;
import com.example.QuanLyQuanCafe.repository.TableBookingRepository;
import com.example.QuanLyQuanCafe.service.BookingStaffReminderService;
import com.example.QuanLyQuanCafe.service.OrderService;

@RestController
@RequestMapping("/api/tables")
public class TablesApiController {

    private final OrderService orderService;
    private final TableBookingRepository tableBookingRepository;
    private final BookingStaffReminderService bookingStaffReminderService;

    public TablesApiController(OrderService orderService,
                               TableBookingRepository tableBookingRepository,
                               BookingStaffReminderService bookingStaffReminderService) {
        this.orderService = orderService;
        this.tableBookingRepository = tableBookingRepository;
        this.bookingStaffReminderService = bookingStaffReminderService;
    }

    private boolean isStaffOrAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_CASHIER")
                        || a.getAuthority().equals("ROLE_SERVER"));
    }

    @GetMapping("/status")
    public ResponseEntity<List<TableStatusDto>> getTableStatus(@RequestParam(value = "total", defaultValue = "20") int totalTables) {
        if (!isStaffOrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (totalTables < 1 || totalTables > 200) {
            return ResponseEntity.badRequest().build();
        }

        Map<Integer, TableStatusDto> result = new HashMap<>();
        for (int i = 1; i <= totalTables; i++) {
            result.put(i, TableStatusDto.available(i));
        }

        // Occupied from open dine-in orders
        List<CafeOrder> orders = orderService.findAll();
        for (CafeOrder o : orders) {
            if (o == null) continue;
            if (o.getType() != OrderType.DINE_IN) continue;
            // Bàn chỉ trả trống khi đơn bị HỦY. Các trạng thái khác vẫn coi là có khách/block.
            if (o.getStatus() == OrderStatus.CANCELLED) continue;
            if (Boolean.TRUE.equals(o.getTableReleased())) continue;

            Integer resolved = o.getTableNumber();
            if (resolved == null) continue;
            int tableNo = resolved;
            if (tableNo < 1 || tableNo > totalTables) continue;

            TableStatusDto current = result.get(tableNo);
            if (current == null || !"OCCUPIED".equals(current.status)) {
                result.put(tableNo, TableStatusDto.occupied(tableNo, o));
                continue;
            }

            // If multiple orders map to same table, keep newest
            if (current.orderCreatedAt != null && o.getCreatedAt() != null && o.getCreatedAt().isAfter(current.orderCreatedAt)) {
                result.put(tableNo, TableStatusDto.occupied(tableNo, o));
            }
        }

        // Reserved: booking CONFIRMED có gán bàn (block cho đến khi tick đã đến hoặc hủy)
        List<TableBooking> reservedBookings = tableBookingRepository.findByReservedTableNumberIsNotNullAndStatus(BookingStatus.CONFIRMED);

        for (TableBooking b : reservedBookings) {
            if (b == null) continue;
            Integer tableNoObj = b.getReservedTableNumber();
            if (tableNoObj == null) continue;
            int tableNo = tableNoObj;
            if (tableNo < 1 || tableNo > totalTables) continue;

            TableStatusDto current = result.get(tableNo);
            if (current != null && "OCCUPIED".equals(current.status)) {
                continue; // occupied takes priority
            }
            result.put(tableNo, TableStatusDto.reserved(tableNo, b));
        }

        List<TableStatusDto> list = new ArrayList<>(result.values());
        list.sort(Comparator.comparingInt(t -> t.number));
        return ResponseEntity.ok(list);
    }

    private static Map<String, Object> orderToSafeJson(CafeOrder o) {
        Map<String, Object> m = new HashMap<>();
        if (o == null) return m;
        m.put("id", o.getId());
        m.put("orderCode", o.getOrderCode());
        m.put("customerName", o.getCustomerName());
        m.put("customerPhone", o.getCustomerPhone());
        m.put("type", o.getType() != null ? o.getType().name() : null);
        m.put("tableNumber", o.getTableNumber());
        m.put("orderNote", o.getOrderNote());
        m.put("tableSummary", o.getTableAndNoteColumn());
        m.put("status", o.getStatus() != null ? o.getStatus().name() : null);
        m.put("subtotal", o.getSubtotal());
        m.put("discount", o.getDiscount());
        m.put("total", o.getTotal());
        m.put("paymentMethod", o.getPaymentMethod() != null ? o.getPaymentMethod().name() : null);
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        m.put("tableReleased", o.getTableReleased());
        return m;
    }

    private static Map<String, Object> orderItemToSafeJson(OrderItem it) {
        Map<String, Object> m = new HashMap<>();
        if (it == null) return m;
        m.put("id", it.getId());
        m.put("itemName", it.getItemName());
        m.put("itemPrice", it.getItemPrice());
        m.put("quantity", it.getQuantity());
        m.put("note", it.getNote());
        return m;
    }

    @GetMapping("/{tableNo}/active-order")
    public ResponseEntity<Map<String, Object>> getActiveOrderForTable(@PathVariable("tableNo") int tableNo) {
        if (!isStaffOrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (tableNo < 1 || tableNo > 200) {
            return ResponseEntity.badRequest().build();
        }

        CafeOrder picked = orderService.findActiveDineInOrderForTable(tableNo);
        if (picked == null) {
            return ResponseEntity.notFound().build();
        }

        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (OrderItem it : orderService.findItemsByOrder(picked)) {
            itemMaps.add(orderItemToSafeJson(it));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("order", orderToSafeJson(picked));
        result.put("items", itemMaps);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{tableNo}/release")
    public ResponseEntity<Map<String, Object>> releaseTable(@PathVariable("tableNo") int tableNo) {
        if (!isStaffOrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (tableNo < 1 || tableNo > 200) {
            return ResponseEntity.badRequest().build();
        }
        CafeOrder order = orderService.findActiveDineInOrderForTable(tableNo);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        order.setTableReleased(Boolean.TRUE);
        CafeOrder saved = orderService.save(order);

        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("orderCode", saved.getOrderCode());
        result.put("tableNo", tableNo);
        return ResponseEntity.ok(result);
    }

    /**
     * Lấy danh sách booking chờ gán bàn (CONFIRMED, chưa gán bàn, giờ đặt từ hôm nay).
     */
    @GetMapping("/pending-bookings")
    public ResponseEntity<List<Map<String, Object>>> getPendingBookings() {
        if (!isStaffOrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        List<TableBooking> pending = tableBookingRepository
                .findByStatusAndReservedTableNumberIsNullAndBookingTimeGreaterThanEqualOrderByBookingTimeAsc(
                        BookingStatus.CONFIRMED, todayStart);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TableBooking b : pending) {
            result.add(bookingToMap(b));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Gán bàn cho booking từ màn hình Quản lý Bàn.
     */
    @PostMapping("/{tableNo}/assign-booking")
    public ResponseEntity<Map<String, Object>> assignBookingToTable(
            @PathVariable("tableNo") int tableNo,
            @RequestParam("bookingId") Long bookingId) {
        if (!isStaffOrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (tableNo < 1 || tableNo > 200 || bookingId == null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Tham số không hợp lệ."));
        }

        // Kiểm tra bàn có đang có đơn không
        if (orderService.findActiveDineInOrderForTable(tableNo) != null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Bàn " + tableNo + " đang có khách."));
        }

        // Kiểm tra bàn đã có booking khác gán chưa
        List<TableBooking> existing = tableBookingRepository.findByReservedTableNumberAndStatus(tableNo, BookingStatus.CONFIRMED);
        if (!existing.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Bàn " + tableNo + " đã được giữ bởi booking khác."));
        }

        var opt = tableBookingRepository.findById(bookingId);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Không tìm thấy booking."));
        }
        TableBooking b = opt.get();
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Booking không ở trạng thái chờ."));
        }

        b.setReservedTableNumber(tableNo);
        tableBookingRepository.save(b);

        return ResponseEntity.ok(Map.of("ok", true, "tableNo", tableNo, "bookingId", bookingId));
    }

    /**
     * Bỏ gán bàn cho booking từ màn hình Quản lý Bàn.
     */
    @PostMapping("/{tableNo}/unassign-booking")
    public ResponseEntity<Map<String, Object>> unassignBookingFromTable(@PathVariable("tableNo") int tableNo) {
        if (!isStaffOrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (tableNo < 1 || tableNo > 200) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Số bàn không hợp lệ."));
        }

        List<TableBooking> bookings = tableBookingRepository.findByReservedTableNumberAndStatus(tableNo, BookingStatus.CONFIRMED);
        if (bookings.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Bàn này không có booking nào đang giữ."));
        }

        for (TableBooking b : bookings) {
            b.setReservedTableNumber(null);
            tableBookingRepository.save(b);
        }

        return ResponseEntity.ok(Map.of("ok", true, "tableNo", tableNo));
    }

    /**
     * Tick đã đến cho booking từ màn hình Quản lý Bàn.
     */
    @PostMapping("/{tableNo}/checkin-booking")
    public ResponseEntity<Map<String, Object>> checkinBookingFromTable(@PathVariable("tableNo") int tableNo) {
        if (!isStaffOrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (tableNo < 1 || tableNo > 200) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Số bàn không hợp lệ."));
        }

        List<TableBooking> bookings = tableBookingRepository.findByReservedTableNumberAndStatus(tableNo, BookingStatus.CONFIRMED);
        if (bookings.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Bàn này không có booking nào đang giữ."));
        }

        TableBooking b = bookings.get(0);
        var err = bookingStaffReminderService.applyStaffBookingAction(b.getId(), "CHECK_IN", null);
        if (err.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", err.get()));
        }

        return ResponseEntity.ok(Map.of("ok", true, "tableNo", tableNo, "bookingId", b.getId()));
    }

    /**
     * Hủy booking từ màn hình Quản lý Bàn.
     */
    @PostMapping("/{tableNo}/cancel-booking")
    public ResponseEntity<Map<String, Object>> cancelBookingFromTable(
            @PathVariable("tableNo") int tableNo,
            @RequestParam(value = "note", required = false) String note) {
        if (!isStaffOrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (tableNo < 1 || tableNo > 200) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Số bàn không hợp lệ."));
        }

        List<TableBooking> bookings = tableBookingRepository.findByReservedTableNumberAndStatus(tableNo, BookingStatus.CONFIRMED);
        if (bookings.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Bàn này không có booking nào đang giữ."));
        }

        TableBooking b = bookings.get(0);
        var err = bookingStaffReminderService.applyStaffBookingAction(b.getId(), "CANCEL", note);
        if (err.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", err.get()));
        }

        return ResponseEntity.ok(Map.of("ok", true, "tableNo", tableNo, "bookingId", b.getId()));
    }

    private static Map<String, Object> bookingToMap(TableBooking b) {
        Map<String, Object> m = new HashMap<>();
        if (b == null) return m;
        m.put("id", b.getId());
        m.put("name", b.getName());
        m.put("phone", b.getPhone());
        m.put("email", b.getEmail());
        m.put("bookingTime", b.getBookingTime() != null ? b.getBookingTime().toString() : null);
        m.put("guests", b.getGuests());
        m.put("note", b.getNote());
        m.put("reservedTableNumber", b.getReservedTableNumber());
        m.put("status", b.getStatus() != null ? b.getStatus().name() : null);
        return m;
    }

    public static class TableStatusDto {
        public int number;
        public String label;
        public String status; // AVAILABLE | RESERVED | OCCUPIED

        // order fields
        public String orderCode;
        public String orderStatus;
        public String orderCustomerName;
        public LocalDateTime orderCreatedAt;

        // booking fields
        public Long bookingId;
        public String bookingName;
        public String bookingPhone;
        public LocalDateTime bookingTime;

        public static TableStatusDto available(int number) {
            TableStatusDto dto = new TableStatusDto();
            dto.number = number;
            dto.label = "Bàn " + number;
            dto.status = "AVAILABLE";
            return dto;
        }

        public static TableStatusDto occupied(int number, CafeOrder o) {
            TableStatusDto dto = new TableStatusDto();
            dto.number = number;
            dto.label = "Bàn " + number;
            dto.status = "OCCUPIED";
            dto.orderCode = o.getOrderCode();
            dto.orderStatus = o.getStatus() != null ? o.getStatus().name() : null;
            dto.orderCustomerName = o.getCustomerName();
            dto.orderCreatedAt = o.getCreatedAt();
            return dto;
        }

        public static TableStatusDto reserved(int number, TableBooking b) {
            TableStatusDto dto = new TableStatusDto();
            dto.number = number;
            dto.label = "Bàn " + number;
            dto.status = "RESERVED";
            dto.bookingId = b.getId();
            dto.bookingName = b.getName();
            dto.bookingPhone = b.getPhone();
            dto.bookingTime = b.getBookingTime();
            return dto;
        }
    }
}


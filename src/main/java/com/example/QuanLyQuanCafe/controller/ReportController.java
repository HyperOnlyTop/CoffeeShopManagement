package com.example.QuanLyQuanCafe.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.OrderItem;
import com.example.QuanLyQuanCafe.model.OrderStatus;
import com.example.QuanLyQuanCafe.service.OrderService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final OrderService orderService;

    public ReportController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping(value = "/revenue", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> exportRevenueSummary(@RequestParam(value = "days", defaultValue = "7") int days) {
        int clamped = Math.min(366, Math.max(1, days));
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(clamped - 1);
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = today.atTime(LocalTime.MAX);

        List<CafeOrder> orders = orderService.findByCreatedAtBetween(from, to);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        int completedOrders = 0;
        int cancelledOrders = 0;
        int pendingOrders = 0;

        for (CafeOrder o : orders) {
            if (o.getStatus() == OrderStatus.CANCELLED) {
                cancelledOrders++;
                continue;
            }
            if (o.getStatus() == OrderStatus.PENDING) {
                pendingOrders++;
            }
            boolean revenueBearing = o.getStatus() == null || o.getStatus() == OrderStatus.COMPLETED;
            if (revenueBearing) {
                completedOrders++;
                totalRevenue = totalRevenue.add(o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO);
                List<OrderItem> items = orderService.findItemsByOrder(o);
                if (items != null) {
                    for (OrderItem item : items) {
                        BigDecimal unitCost = item.getItemCost();
                        int q = item.getQuantity() != null ? item.getQuantity() : 0;
                        if (unitCost != null && q > 0) {
                            totalCost = totalCost.add(unitCost.multiply(BigDecimal.valueOf(q)));
                        }
                    }
                }
            }
        }

        BigDecimal grossProfit = totalRevenue.subtract(totalCost);
        BigDecimal marginPercent = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            marginPercent = grossProfit.multiply(BigDecimal.valueOf(100))
                    .divide(totalRevenue, 1, RoundingMode.HALF_UP);
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fileFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // BOM for Excel UTF-8

        sb.append("BÁO CÁO DOANH THU\n");
        sb.append("Kỳ báo cáo:;").append(fromDate.format(dateFormatter)).append(" - ").append(today.format(dateFormatter)).append("\n");
        sb.append("Số ngày:;").append(clamped).append("\n");
        sb.append("\n");

        sb.append("CHỈ SỐ;GIÁ TRỊ\n");
        sb.append("Tổng doanh thu;").append(formatMoney(totalRevenue)).append("\n");
        sb.append("Tổng giá vốn (COGS);").append(formatMoney(totalCost)).append("\n");
        sb.append("Lãi gộp;").append(formatMoney(grossProfit)).append("\n");
        sb.append("Biên lợi nhuận gộp;").append(marginPercent.toPlainString()).append("%\n");
        sb.append("\n");

        sb.append("THỐNG KÊ ĐƠN HÀNG;SỐ LƯỢNG\n");
        sb.append("Tổng số đơn;").append(orders.size()).append("\n");
        sb.append("Đơn hoàn thành;").append(completedOrders).append("\n");
        sb.append("Đơn chờ xử lý;").append(pendingOrders).append("\n");
        sb.append("Đơn đã hủy;").append(cancelledOrders).append("\n");
        sb.append("\n");

        sb.append("CHI TIẾT ĐƠN HÀNG (chỉ đơn tính doanh thu)\n");
        sb.append("Mã đơn;Khách hàng;SĐT;Tổng tiền;Trạng thái;Ngày tạo\n");

        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (CafeOrder order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }
            boolean revenueBearing = order.getStatus() == null || order.getStatus() == OrderStatus.COMPLETED;
            if (!revenueBearing) {
                continue;
            }

            String code = order.getOrderCode() != null ? order.getOrderCode() : "";
            String name = order.getCustomerName() != null ? order.getCustomerName() : "Khách vãng lai";
            String phone = order.getCustomerPhone() != null ? order.getCustomerPhone() : "";
            BigDecimal total = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
            String statusText = order.getStatus() != null ? translateStatus(order.getStatus()) : "";
            String createdAt = order.getCreatedAt() != null ? order.getCreatedAt().format(dtFormatter) : "";

            sb.append(code).append(';')
              .append(name).append(';')
              .append(phone).append(';')
              .append(formatMoney(total)).append(';')
              .append(statusText).append(';')
              .append(createdAt).append('\n');
        }

        String filename = "bao-cao-doanh-thu-" + clamped + "-ngay-" + today.format(fileFormatter) + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));

        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/orders-7-days", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> exportOrdersLast7Days() {
        return exportRevenueSummary(7);
    }

    private static String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return String.format("%,.0f đ", value);
    }

    private static String translateStatus(OrderStatus status) {
        if (status == null) return "";
        return switch (status) {
            case COMPLETED -> "Hoàn thành";
            case PENDING -> "Chờ xử lý";
            case CANCELLED -> "Đã hủy";
        };
    }
}

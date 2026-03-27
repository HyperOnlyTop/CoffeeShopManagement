package com.example.QuanLyQuanCafe.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.OrderStatus;
import com.example.QuanLyQuanCafe.service.OrderService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final OrderService orderService;

    public ReportController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping(value = "/orders-7-days", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportOrdersLast7Days() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(6).atStartOfDay();
        LocalDateTime to = today.atTime(LocalTime.MAX);

        List<CafeOrder> orders = orderService.findByCreatedAtBetween(from, to);

        StringBuilder sb = new StringBuilder();
        sb.append("OrderCode,CustomerName,CustomerPhone,Total,Status,CreatedAt\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (CafeOrder order : orders) {
            String code = order.getOrderCode() != null ? order.getOrderCode() : "";
            String name = order.getCustomerName() != null ? order.getCustomerName() : "";
            String phone = order.getCustomerPhone() != null ? order.getCustomerPhone() : "";
            BigDecimal total = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
            OrderStatus status = order.getStatus();
            String statusText = status != null ? status.name() : "";
            String createdAt = order.getCreatedAt() != null ? order.getCreatedAt().format(formatter) : "";

            sb.append(escape(code)).append(',')
              .append(escape(name)).append(',')
              .append(escape(phone)).append(',')
              .append(total.toPlainString()).append(',')
              .append(escape(statusText)).append(',')
              .append(escape(createdAt)).append('\n');
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders-last-7-days.csv");
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");

        return new ResponseEntity<>(sb.toString(), headers, HttpStatus.OK);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }
}

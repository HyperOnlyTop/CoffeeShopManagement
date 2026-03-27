package com.example.QuanLyQuanCafe.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.DailyRevenue;
import com.example.QuanLyQuanCafe.model.OrderStatus;
import com.example.QuanLyQuanCafe.service.DailyRevenueService;
import com.example.QuanLyQuanCafe.service.OrderService;

@RestController
@RequestMapping("/api/revenue")
public class DailyRevenueController {

    private final DailyRevenueService dailyRevenueService;
    private final OrderService orderService;

    public DailyRevenueController(DailyRevenueService dailyRevenueService, OrderService orderService) {
        this.dailyRevenueService = dailyRevenueService;
        this.orderService = orderService;
    }

    @GetMapping("/daily")
    public List<DailyRevenue> getAll() {
        return getLast7DaysRevenue();
    }

    @GetMapping("/daily/{date}")
    public ResponseEntity<DailyRevenue> getByDate(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyRevenue revenue = dailyRevenueService.findByDate(date);
        if (revenue == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/summary-7-days")
    public Summary7DaysResponse getSummary7Days() {
        List<DailyRevenue> days = getLast7DaysRevenue();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalOrders = 0;
        BigDecimal averageOrderValue = BigDecimal.ZERO;

        LocalDate bestDay = null;
        BigDecimal bestDayRevenue = BigDecimal.ZERO;

        for (DailyRevenue dr : days) {
            if (dr.getTotalRevenue() != null) {
                totalRevenue = totalRevenue.add(dr.getTotalRevenue());
                if (dr.getTotalRevenue().compareTo(bestDayRevenue) > 0) {
                    bestDayRevenue = dr.getTotalRevenue();
                    bestDay = dr.getRevenueDate();
                }
            }
            if (dr.getTotalOrders() != null) {
                totalOrders += dr.getTotalOrders();
            }
        }

        if (totalOrders > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, java.math.RoundingMode.HALF_UP);
        }

        String bestDayLabel = null;
        if (bestDay != null) {
            DayOfWeek dow = bestDay.getDayOfWeek();
            switch (dow) {
                case MONDAY:
                    bestDayLabel = "T2";
                    break;
                case TUESDAY:
                    bestDayLabel = "T3";
                    break;
                case WEDNESDAY:
                    bestDayLabel = "T4";
                    break;
                case THURSDAY:
                    bestDayLabel = "T5";
                    break;
                case FRIDAY:
                    bestDayLabel = "T6";
                    break;
                case SATURDAY:
                    bestDayLabel = "T7";
                    break;
                case SUNDAY:
                    bestDayLabel = "CN";
                    break;
                default:
                    bestDayLabel = bestDay.toString();
            }
        }

        Summary7DaysResponse response = new Summary7DaysResponse();
        response.setTotalRevenue(totalRevenue);
        response.setTotalOrders(totalOrders);
        response.setAverageOrderValue(averageOrderValue);
        response.setBestDayLabel(bestDayLabel);
        response.setBestDayRevenue(bestDayRevenue);

        return response;
    }

    private List<DailyRevenue> getLast7DaysRevenue() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);

        List<DailyRevenue> existing = dailyRevenueService.findAll();
        if (!existing.isEmpty()) {
            List<DailyRevenue> filtered = new ArrayList<>();
            for (DailyRevenue dr : existing) {
                if (dr.getRevenueDate() == null) {
                    continue;
                }
                LocalDate d = dr.getRevenueDate();
                if (!d.isBefore(from) && !d.isAfter(today)) {
                    filtered.add(dr);
                }
            }
            filtered.sort(Comparator.comparing(DailyRevenue::getRevenueDate));
            return filtered;
        }

        List<DailyRevenue> generated = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            List<CafeOrder> orders = orderService.findByCreatedAtBetween(start, end);

            BigDecimal totalRevenue = orders.stream()
                    .filter(o -> o.getStatus() == null || o.getStatus() == OrderStatus.COMPLETED)
                    .map(o -> o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            DailyRevenue dr = new DailyRevenue();
            dr.setRevenueDate(date);
            dr.setTotalOrders(orders.size());
            dr.setTotalRevenue(totalRevenue);

            generated.add(dr);
        }

        return generated;
    }

    public static class Summary7DaysResponse {
        private BigDecimal totalRevenue;
        private int totalOrders;
        private BigDecimal averageOrderValue;
        private String bestDayLabel;
        private BigDecimal bestDayRevenue;

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public int getTotalOrders() {
            return totalOrders;
        }

        public void setTotalOrders(int totalOrders) {
            this.totalOrders = totalOrders;
        }

        public BigDecimal getAverageOrderValue() {
            return averageOrderValue;
        }

        public void setAverageOrderValue(BigDecimal averageOrderValue) {
            this.averageOrderValue = averageOrderValue;
        }

        public String getBestDayLabel() {
            return bestDayLabel;
        }

        public void setBestDayLabel(String bestDayLabel) {
            this.bestDayLabel = bestDayLabel;
        }

        public BigDecimal getBestDayRevenue() {
            return bestDayRevenue;
        }

        public void setBestDayRevenue(BigDecimal bestDayRevenue) {
            this.bestDayRevenue = bestDayRevenue;
        }
    }
}

package com.example.QuanLyQuanCafe.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.QuanLyQuanCafe.model.OrderItem;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.DailyRevenue;
import com.example.QuanLyQuanCafe.model.OrderStatus;
import com.example.QuanLyQuanCafe.service.DailyRevenueService;
import com.example.QuanLyQuanCafe.service.OrderService;
import com.example.QuanLyQuanCafe.service.OrderService.RevenueWindowSummary;

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
    public List<DailyRevenue> getAll(@org.springframework.web.bind.annotation.RequestParam(value = "days", defaultValue = "7") int days) {
        return getLastDaysRevenue(days);
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

    @GetMapping("/summary")
    public Summary7DaysResponse getSummary(@RequestParam(value = "days", defaultValue = "7") int days) {
        int clamped = Math.min(366, Math.max(1, days));
        RevenueWindowSummary s = orderService.summarizeRevenueWindowDays(clamped);

        Summary7DaysResponse response = new Summary7DaysResponse();
        response.setTotalRevenue(s.getTotalRevenue());
        response.setTotalCost(s.getTotalCost());
        response.setGrossProfit(s.getGrossProfit());
        response.setGrossMarginPercent(s.getGrossMarginPercent());
        response.setTotalOrders(s.getTotalOrders());
        response.setAverageOrderValue(s.getAverageOrderValue());
        response.setBestDayRevenue(s.getBestDayRevenue());
        response.setBestDayLabel(formatDayLabel(s.getBestDay()));

        return response;
    }

    @GetMapping("/summary-7-days")
    public Summary7DaysResponse getSummary7Days() {
        return getSummary(7);
    }

    @GetMapping("/by-category")
    public List<CategoryRevenueItem> getRevenueByCategory(@RequestParam(value = "days", defaultValue = "7") int days) {
        int clamped = Math.min(366, Math.max(1, days));
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(clamped - 1).atStartOfDay();
        LocalDateTime to = today.atTime(LocalTime.MAX);

        List<CafeOrder> orders = orderService.findByCreatedAtBetween(from, to);

        Map<String, BigDecimal> categoryRevenue = new HashMap<>();
        Map<String, BigDecimal> categoryCost = new HashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (CafeOrder o : orders) {
            if (o.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }
            boolean revenueBearing = o.getStatus() == null || o.getStatus() == OrderStatus.COMPLETED;
            if (!revenueBearing) {
                continue;
            }
            List<OrderItem> items = orderService.findItemsByOrder(o);
            if (items == null) continue;
            for (OrderItem item : items) {
                BigDecimal lineTotal = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                BigDecimal lineCost = BigDecimal.ZERO;
                if (item.getItemCost() != null && item.getQuantity() != null) {
                    lineCost = item.getItemCost().multiply(BigDecimal.valueOf(item.getQuantity()));
                }

                totalRevenue = totalRevenue.add(lineTotal);

                String catName = "Khác";
                if (item.getMenuItem() != null && item.getMenuItem().getCategory() != null
                        && item.getMenuItem().getCategory().getName() != null) {
                    catName = item.getMenuItem().getCategory().getName();
                }
                categoryRevenue.merge(catName, lineTotal, BigDecimal::add);
                categoryCost.merge(catName, lineCost, BigDecimal::add);
            }
        }

        List<CategoryRevenueItem> result = new ArrayList<>();
        final BigDecimal finalTotal = totalRevenue;
        for (Map.Entry<String, BigDecimal> e : categoryRevenue.entrySet()) {
            BigDecimal rev = e.getValue();
            BigDecimal cost = categoryCost.getOrDefault(e.getKey(), BigDecimal.ZERO);
            BigDecimal profit = rev.subtract(cost);

            BigDecimal pct = BigDecimal.ZERO;
            if (finalTotal.compareTo(BigDecimal.ZERO) > 0) {
                pct = rev.multiply(BigDecimal.valueOf(100))
                        .divide(finalTotal, 1, RoundingMode.HALF_UP);
            }

            BigDecimal marginPct = BigDecimal.ZERO;
            if (rev.compareTo(BigDecimal.ZERO) > 0) {
                marginPct = profit.multiply(BigDecimal.valueOf(100))
                        .divide(rev, 1, RoundingMode.HALF_UP);
            }

            result.add(new CategoryRevenueItem(e.getKey(), rev, cost, profit, pct, marginPct));
        }
        result.sort(Comparator.comparing(CategoryRevenueItem::getProfit).reversed());
        return result;
    }

    private static String formatDayLabel(LocalDate bestDay) {
        if (bestDay == null) {
            return null;
        }
        return switch (bestDay.getDayOfWeek()) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }

    private List<DailyRevenue> getLastDaysRevenue(int daysCount) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(daysCount - 1);

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

        for (int i = daysCount - 1; i >= 0; i--) {
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
        private BigDecimal totalCost;
        private BigDecimal grossProfit;
        private BigDecimal grossMarginPercent;
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

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
        }

        public BigDecimal getGrossProfit() {
            return grossProfit;
        }

        public void setGrossProfit(BigDecimal grossProfit) {
            this.grossProfit = grossProfit;
        }

        public BigDecimal getGrossMarginPercent() {
            return grossMarginPercent;
        }

        public void setGrossMarginPercent(BigDecimal grossMarginPercent) {
            this.grossMarginPercent = grossMarginPercent;
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

    public static class CategoryRevenueItem {
        private final String category;
        private final BigDecimal revenue;
        private final BigDecimal cost;
        private final BigDecimal profit;
        private final BigDecimal percent;
        private final BigDecimal marginPercent;

        public CategoryRevenueItem(String category, BigDecimal revenue, BigDecimal cost,
                                   BigDecimal profit, BigDecimal percent, BigDecimal marginPercent) {
            this.category = category;
            this.revenue = revenue;
            this.cost = cost;
            this.profit = profit;
            this.percent = percent;
            this.marginPercent = marginPercent;
        }

        public String getCategory() {
            return category;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public BigDecimal getCost() {
            return cost;
        }

        public BigDecimal getProfit() {
            return profit;
        }

        public BigDecimal getPercent() {
            return percent;
        }

        public BigDecimal getMarginPercent() {
            return marginPercent;
        }
    }
}

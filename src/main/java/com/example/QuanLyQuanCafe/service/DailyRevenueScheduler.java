package com.example.QuanLyQuanCafe.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.DailyRevenue;
import com.example.QuanLyQuanCafe.model.OrderStatus;

@Service
public class DailyRevenueScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DailyRevenueScheduler.class);

    private final OrderService orderService;
    private final DailyRevenueService dailyRevenueService;

    public DailyRevenueScheduler(OrderService orderService, DailyRevenueService dailyRevenueService) {
        this.orderService = orderService;
        this.dailyRevenueService = dailyRevenueService;
    }

    /**
     * Chạy mỗi ngày lúc 00:05 để chốt doanh thu của ngày hôm trước
     * và lưu vào bảng daily_revenue.
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void aggregateYesterdayRevenue() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        aggregateForDate(targetDate);
    }

    /**
     * Tính toán và lưu doanh thu cho một ngày cụ thể.
     * Có thể tái sử dụng nếu cần chạy lại cho một ngày bất kỳ.
     */
    public void aggregateForDate(LocalDate date) {
        if (date == null) {
            return;
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<CafeOrder> orders = orderService.findByCreatedAtBetween(start, end);

        int totalOrders = orders.size();

        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == null || o.getStatus() == OrderStatus.COMPLETED)
                .map(o -> o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (totalOrders > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, java.math.RoundingMode.HALF_UP);
        }

        // Hiện tại chưa có cột ngày tạo khách hàng, nên tạm thời
        // để newCustomers = 0. Có thể cải tiến sau nếu cần.
        int newCustomers = 0;

        DailyRevenue existing = dailyRevenueService.findByDate(date);
        DailyRevenue dailyRevenue = existing != null ? existing : new DailyRevenue();

        dailyRevenue.setRevenueDate(date);
        dailyRevenue.setTotalOrders(totalOrders);
        dailyRevenue.setTotalRevenue(totalRevenue);
        dailyRevenue.setAverageOrderValue(averageOrderValue);
        dailyRevenue.setNewCustomers(newCustomers);

        dailyRevenueService.save(dailyRevenue);

        LOGGER.info("Aggregated daily revenue for date {}: totalOrders={}, totalRevenue={}",
                date, totalOrders, totalRevenue);
    }
}

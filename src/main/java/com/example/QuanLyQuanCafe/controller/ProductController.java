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

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.QuanLyQuanCafe.model.StaffStatus;
import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.DailyRevenue;
import com.example.QuanLyQuanCafe.model.OrderItem;
import com.example.QuanLyQuanCafe.model.OrderStatus;
import com.example.QuanLyQuanCafe.model.Customer;
import com.example.QuanLyQuanCafe.service.MenuService;
import com.example.QuanLyQuanCafe.service.OrderService;
import com.example.QuanLyQuanCafe.service.StaffService;
import com.example.QuanLyQuanCafe.service.DailyRevenueService;
import com.example.QuanLyQuanCafe.repository.CustomerRepository;

@Controller
public class ProductController {

	private final MenuService menuService;
	private final OrderService orderService;
    private final StaffService staffService;
	private final DailyRevenueService dailyRevenueService;
	private final CustomerRepository customerRepository;

	public ProductController(
			MenuService menuService,
			OrderService orderService,
			StaffService staffService,
			DailyRevenueService dailyRevenueService,
			CustomerRepository customerRepository) {
		this.menuService = menuService;
		this.orderService = orderService;
        this.staffService = staffService;
		this.dailyRevenueService = dailyRevenueService;
		this.customerRepository = customerRepository;
	}

	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		LocalDate today = LocalDate.now();

		DailyRevenue todayRevenueRow = dailyRevenueService.findByDate(today);
		BigDecimal todayRevenue = BigDecimal.ZERO;
		int todayOrders = 0;
		int todayNewCustomers = 0;
		BigDecimal todayAvgOrderValue = BigDecimal.ZERO;

		if (todayRevenueRow != null) {
			if (todayRevenueRow.getTotalRevenue() != null) {
				todayRevenue = todayRevenueRow.getTotalRevenue();
			}
			if (todayRevenueRow.getTotalOrders() != null) {
				todayOrders = todayRevenueRow.getTotalOrders();
			}
			if (todayRevenueRow.getNewCustomers() != null) {
				todayNewCustomers = todayRevenueRow.getNewCustomers();
			}
			if (todayRevenueRow.getAverageOrderValue() != null) {
				todayAvgOrderValue = todayRevenueRow.getAverageOrderValue();
			}
		} else {
			LocalDateTime startOfDay = today.atStartOfDay();
			LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
			List<CafeOrder> todaysOrders = orderService.findByCreatedAtBetween(startOfDay, endOfDay);
			todayOrders = todaysOrders.size();
			todayRevenue = todaysOrders.stream()
					.filter(o -> o.getStatus() == null || o.getStatus() == OrderStatus.COMPLETED)
					.map(o -> o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			if (todayOrders > 0) {
				todayAvgOrderValue = todayRevenue.divide(BigDecimal.valueOf(todayOrders), 0, RoundingMode.HALF_UP);
			}
		}

		BigDecimal monthRevenue = BigDecimal.ZERO;
		List<DailyRevenue> allRevenue = dailyRevenueService.findAll();
		for (DailyRevenue dr : allRevenue) {
			if (dr.getRevenueDate() != null
					&& dr.getRevenueDate().getYear() == today.getYear()
					&& dr.getRevenueDate().getMonth() == today.getMonth()
					&& dr.getTotalRevenue() != null) {
				monthRevenue = monthRevenue.add(dr.getTotalRevenue());
			}
		}

		long totalOrders = orderService.countAll();
		long totalCustomers = customerRepository.count();

		List<CafeOrder> recentOrdersRaw = orderService.findRecent(5);
		List<RecentOrderView> recentOrders = new ArrayList<>();
		for (CafeOrder order : recentOrdersRaw) {
			List<OrderItem> items = orderService.findItemsByOrder(order);
			int itemsCount = items != null ? items.size() : 0;
			recentOrders.add(new RecentOrderView(order, itemsCount));
		}

		LocalDateTime sevenDaysAgo = today.minusDays(6).atStartOfDay();
		LocalDateTime endOfToday = today.atTime(LocalTime.MAX);
		List<CafeOrder> ordersLast7Days = orderService.findByCreatedAtBetween(sevenDaysAgo, endOfToday);
		Map<String, BestSellerAccumulator> bestSellerMap = new HashMap<>();
		for (CafeOrder order : ordersLast7Days) {
			List<OrderItem> items = orderService.findItemsByOrder(order);
			if (items == null) continue;
			for (OrderItem item : items) {
				String name = item.getItemName();
				if (name == null || name.isBlank()) {
					continue;
				}
				BestSellerAccumulator acc = bestSellerMap.computeIfAbsent(name, k -> new BestSellerAccumulator());
				int qty = item.getQuantity() != null ? item.getQuantity() : 0;
				acc.quantity += qty;
				BigDecimal lineTotal = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
				acc.revenue = acc.revenue.add(lineTotal);
			}
		}
		List<BestSellerView> bestSellers = new ArrayList<>();
		for (Map.Entry<String, BestSellerAccumulator> entry : bestSellerMap.entrySet()) {
			bestSellers.add(new BestSellerView(entry.getKey(), entry.getValue().quantity, entry.getValue().revenue));
		}
		bestSellers.sort(Comparator.comparingLong(BestSellerView::getTotalQuantity).reversed());
		if (bestSellers.size() > 5) {
			bestSellers = bestSellers.subList(0, 5);
		}

		model.addAttribute("todayRevenue", todayRevenue);
		model.addAttribute("monthRevenue", monthRevenue);
		model.addAttribute("todayOrders", todayOrders);
		model.addAttribute("totalOrders", totalOrders);
		model.addAttribute("todayNewCustomers", todayNewCustomers);
		model.addAttribute("totalCustomers", totalCustomers);
		model.addAttribute("todayAvgOrderValue", todayAvgOrderValue);
		model.addAttribute("recentOrders", recentOrders);
		model.addAttribute("bestSellers", bestSellers);

		return "dashboard";
	}

	@GetMapping("/Menu")
	public String menu(Model model) {
		model.addAttribute("menuItems", menuService.getAllItems());
		return "Menu";
	}

	@GetMapping("/Order")
	public String order(Model model) {
		model.addAttribute("orders", orderService.findAll());
		return "Order";
	}

	@GetMapping("/Revenue")
	public String revenue() {
		return "Revenue";
	}

	@GetMapping("/Staff")
	public String nhanVien(Model model) {
		var allStaff = staffService.findAll();
		model.addAttribute("staffList", allStaff);
		int total = allStaff.size();
		int working = (int) allStaff.stream()
			.filter(s -> s.getStatus() == StaffStatus.ACTIVE)
			.count();
		int onLeave = (int) allStaff.stream()
			.filter(s -> s.getStatus() == StaffStatus.ON_LEAVE)
			.count();
		int inactive = (int) allStaff.stream()
			.filter(s -> s.getStatus() == StaffStatus.INACTIVE)
			.count();
		model.addAttribute("staffTotal", total);
		model.addAttribute("staffWorking", working);
		model.addAttribute("staffOnLeave", onLeave);
		model.addAttribute("staffInactive", inactive);
		return "Staff";
	}

	@GetMapping("/Inventory")
	public String khoHang() {
		return "Inventory";
	}

	@GetMapping("/Setting")
	public String caiDat() {
		return "Setting";
	}

	public static class RecentOrderView {
		private final CafeOrder order;
		private final int itemsCount;

		public RecentOrderView(CafeOrder order, int itemsCount) {
			this.order = order;
			this.itemsCount = itemsCount;
		}

		public CafeOrder getOrder() {
			return order;
		}

		public int getItemsCount() {
			return itemsCount;
		}
	}

	private static class BestSellerAccumulator {
		long quantity = 0;
		BigDecimal revenue = BigDecimal.ZERO;
	}

	public static class BestSellerView {
		private final String name;
		private final long totalQuantity;
		private final BigDecimal totalRevenue;

		public BestSellerView(String name, long totalQuantity, BigDecimal totalRevenue) {
			this.name = name;
			this.totalQuantity = totalQuantity;
			this.totalRevenue = totalRevenue;
		}

		public String getName() {
			return name;
		}

		public long getTotalQuantity() {
			return totalQuantity;
		}

		public BigDecimal getTotalRevenue() {
			return totalRevenue;
		}
	}
}

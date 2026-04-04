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
import java.util.Collections;
import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.model.StaffStatus;
import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.DailyRevenue;
import com.example.QuanLyQuanCafe.model.OrderItem;
import com.example.QuanLyQuanCafe.model.OrderStatus;
import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.TableBooking;
import com.example.QuanLyQuanCafe.model.BookingStatus;
import com.example.QuanLyQuanCafe.model.OrderType;
import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.model.MenuItemStatus;
import com.example.QuanLyQuanCafe.service.MenuService;
import com.example.QuanLyQuanCafe.service.OrderService;
import com.example.QuanLyQuanCafe.service.StaffService;
import com.example.QuanLyQuanCafe.service.DailyRevenueService;
import com.example.QuanLyQuanCafe.repository.CustomerRepository;
import com.example.QuanLyQuanCafe.repository.StaffRepository;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.repository.TableBookingRepository;

@Controller
public class ProductController {

	private final MenuService menuService;
	private final OrderService orderService;
    private final StaffService staffService;
	private final DailyRevenueService dailyRevenueService;
	private final CustomerRepository customerRepository;
	private final StaffRepository staffRepository;
	private final AppUserRepository appUserRepository;
    private final TableBookingRepository tableBookingRepository;

	public ProductController(
			MenuService menuService,
			OrderService orderService,
			StaffService staffService,
			DailyRevenueService dailyRevenueService,
			CustomerRepository customerRepository,
			StaffRepository staffRepository,
			AppUserRepository appUserRepository,
			TableBookingRepository tableBookingRepository) {
		this.menuService = menuService;
		this.orderService = orderService;
        this.staffService = staffService;
		this.dailyRevenueService = dailyRevenueService;
		this.customerRepository = customerRepository;
		this.staffRepository = staffRepository;
		this.appUserRepository = appUserRepository;
		this.tableBookingRepository = tableBookingRepository;
	}

	@GetMapping("/dashboard")
	public String dashboard(Model model, Principal principal) {
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

		return "admin/dashboard";
	}

	@GetMapping("/Menu")
	public String menu(Model model) {
		List<MenuItem> menuItems = menuService.getAllItems();
		model.addAttribute("menuItems", menuItems);
		long menuTotal = menuItems.size();
		long menuAvailable = menuItems.stream().filter(m -> m != null && m.getStatus() == MenuItemStatus.AVAILABLE).count();
		long menuUnavailable = menuItems.stream().filter(m -> m != null && m.getStatus() == MenuItemStatus.UNAVAILABLE).count();
		long menuCategoryCount = menuItems.stream()
				.map(m -> m != null && m.getCategory() != null ? m.getCategory().getName() : "")
				.filter(s -> !s.isBlank())
				.distinct()
				.count();
		model.addAttribute("menuStatTotal", menuTotal);
		model.addAttribute("menuStatAvailable", menuAvailable);
		model.addAttribute("menuStatUnavailable", menuUnavailable);
		model.addAttribute("menuStatCategories", menuCategoryCount);
		return "admin/Menu";
	}

	@GetMapping("/Order")
	public String order(Model model) {
		List<CafeOrder> orders = orderService.findAll();
		model.addAttribute("orders", orders);
		long orderTotal = orders.size();
		long orderPending = orders.stream().filter(o -> o != null && o.getStatus() == OrderStatus.PENDING).count();
		long orderCompleted = orders.stream().filter(o -> o != null && o.getStatus() == OrderStatus.COMPLETED).count();
		long orderCancelled = orders.stream().filter(o -> o != null && o.getStatus() == OrderStatus.CANCELLED).count();
		model.addAttribute("orderStatTotal", orderTotal);
		model.addAttribute("orderStatPending", orderPending);
		model.addAttribute("orderStatCompleted", orderCompleted);
		model.addAttribute("orderStatCancelled", orderCancelled);
		return "admin/Order";
	}

	@GetMapping("/Booking")
	public String bookingList(Model model) {
		List<TableBooking> bookings = tableBookingRepository.findAll();
		bookings.sort(Comparator.comparing(
			TableBooking::getBookingTime,
			Comparator.nullsLast(Comparator.naturalOrder())
		).reversed());
		model.addAttribute("bookings", bookings);
		long bookingTotal = bookings.size();
		long bookingConfirmed = bookings.stream().filter(b -> b != null && b.getStatus() == BookingStatus.CONFIRMED).count();
		long bookingCancelled = bookings.stream().filter(b -> b != null && b.getStatus() == BookingStatus.CANCELLED).count();
		int bookingGuestsSum = bookings.stream().filter(b -> b != null).mapToInt(TableBooking::getGuests).sum();
		model.addAttribute("bookingStatTotal", bookingTotal);
		model.addAttribute("bookingStatConfirmed", bookingConfirmed);
		model.addAttribute("bookingStatCancelled", bookingCancelled);
		model.addAttribute("bookingStatGuestsSum", bookingGuestsSum);
		model.addAttribute("bookingPageNow", LocalDateTime.now());
		return "admin/Booking";
	}

	@GetMapping("/Tables")
	public String tableManagement(Model model) {
		final int totalTables = 20;
		List<TableView> tables = new ArrayList<>();
		for (int i = 1; i <= totalTables; i++) {
			tables.add(TableView.free(i));
		}

		// Xác định các bàn đang có khách dựa theo đơn "tại bàn" chưa hoàn thành/chưa hủy
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

			TableView tv = tables.get(tableNo - 1);
			// Nếu có nhiều đơn cùng 1 bàn, ưu tiên đơn mới hơn (createdAt)
			if (!tv.occupied || (tv.createdAt != null && o.getCreatedAt() != null && o.getCreatedAt().isAfter(tv.createdAt))) {
				tables.set(tableNo - 1, TableView.occupied(
						tableNo,
						o.getOrderCode(),
						o.getCustomerName(),
						o.getStatus() != null ? o.getStatus().name() : "UNKNOWN",
						o.getCreatedAt()
				));
			}
		}

		// Xác định bàn đã được giữ chỗ theo booking CONFIRMED trong cửa sổ giữ: [now-15p, now+15p]
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime holdWindowStart = now.minusMinutes(com.example.QuanLyQuanCafe.config.BookingPolicy.GRACE_AFTER_MINUTES);
		LocalDateTime holdWindowEnd = now.plusMinutes(com.example.QuanLyQuanCafe.config.BookingPolicy.HOLD_BEFORE_MINUTES);
		List<TableBooking> activeBookings = tableBookingRepository.findByStatusAndBookingTimeBetween(
				com.example.QuanLyQuanCafe.model.BookingStatus.CONFIRMED,
				holdWindowStart,
				holdWindowEnd
		);

		for (TableBooking b : activeBookings) {
			if (b == null) continue;
			Integer tableNoObj = b.getReservedTableNumber();
			if (tableNoObj == null) continue; // booking web không chọn bàn; nhân viên gán ở admin
			int tableNo = tableNoObj;
			if (tableNo < 1 || tableNo > totalTables) continue;

			TableView tv = tables.get(tableNo - 1);
			if (tv == null) continue;
			if (tv.occupied) continue; // ưu tiên đỏ nếu đang có khách

			tables.set(tableNo - 1, TableView.reserved(
					tableNo,
					b.getName(),
					b.getPhone(),
					b.getBookingTime()
			));
		}

		long occupiedCount = tables.stream().filter(t -> t != null && t.occupied).count();
		long reservedCount = tables.stream().filter(t -> t != null && t.reserved && !t.occupied).count();
		model.addAttribute("tables", tables);
		model.addAttribute("totalTables", totalTables);
		model.addAttribute("occupiedCount", occupiedCount);
		model.addAttribute("reservedCount", reservedCount);
		return "admin/Tables";
	}

	@GetMapping("/Revenue")
	public String revenue() {
		return "admin/Revenue";
	}

	@GetMapping("/Staff")
	public String nhanVien(Model model, Principal principal) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		boolean isAdmin = auth != null && auth.getAuthorities().stream()
			.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

		List<Staff> staffList;
		if (isAdmin) {
			staffList = staffService.findAll();
		} else {
			staffList = Collections.emptyList();
			if (principal != null) {
				AppUser appUser = appUserRepository.findByUsername(principal.getName());
				if (appUser != null) {
					Staff staff = null;
					if (appUser.getFullName() != null && !appUser.getFullName().isBlank()) {
						staff = staffRepository.findByName(appUser.getFullName());
					}
					if (staff == null) {
						staff = new Staff();
						staff.setName(appUser.getFullName() != null && !appUser.getFullName().isBlank()
								? appUser.getFullName()
								: appUser.getUsername());
						staff.setPhone(appUser.getPhone());
					}
					// Đồng bộ avatar từ tài khoản đăng nhập cho view nhân viên tự xem
					staff.setAvatarUrl(appUser.getAvatarUrl());
					staffList = Collections.singletonList(staff);
				}
			}
		}

		int total = staffList.size();
		int working = (int) staffList.stream()
			.filter(s -> s.getStatus() == StaffStatus.ACTIVE)
			.count();
		int onLeave = (int) staffList.stream()
			.filter(s -> s.getStatus() == StaffStatus.ON_LEAVE)
			.count();
		int inactive = (int) staffList.stream()
			.filter(s -> s.getStatus() == StaffStatus.INACTIVE)
			.count();

		model.addAttribute("staffList", staffList);
		model.addAttribute("staffTotal", total);
		model.addAttribute("staffWorking", working);
		model.addAttribute("staffOnLeave", onLeave);
		model.addAttribute("staffInactive", inactive);
		return "admin/Staff";
	}

	@GetMapping("/Inventory")
	public String khoHang() {
		return "admin/Inventory";
	}

	@GetMapping("/Setting")
	public String caiDat() {
		return "admin/Setting";
	}

	@GetMapping("/Accounts")
	public String quanLyTaiKhoan() {
		return "admin/Accounts";
	}

	@GetMapping("/Customers")
	public String customers(Model model) {
		return "redirect:/Accounts";
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

	public static class TableView {
		public final int number;
		public final String label;
		public final boolean occupied;
		public final boolean reserved;
		public final String orderCode;
		public final String customerName;
		public final String statusLabel;
		public final LocalDateTime createdAt;
		public final LocalDateTime bookingTime;
		public final String bookingPhone;

		private TableView(
				int number,
				boolean occupied,
				boolean reserved,
				String orderCode,
				String customerName,
				String statusLabel,
				LocalDateTime createdAt,
				LocalDateTime bookingTime,
				String bookingPhone
		) {
			this.number = number;
			this.label = "Bàn " + number;
			this.occupied = occupied;
			this.reserved = reserved;
			this.orderCode = orderCode;
			this.customerName = customerName;
			this.statusLabel = statusLabel;
			this.createdAt = createdAt;
			this.bookingTime = bookingTime;
			this.bookingPhone = bookingPhone;
		}

		public static TableView free(int number) {
			return new TableView(number, false, false, null, null, null, null, null, null);
		}

		public static TableView occupied(int number, String orderCode, String customerName, String statusLabel, LocalDateTime createdAt) {
			return new TableView(number, true, false, orderCode, customerName, statusLabel, createdAt, null, null);
		}

		public static TableView reserved(int number, String customerName, String bookingPhone, LocalDateTime bookingTime) {
			return new TableView(number, false, true, null, customerName, "RESERVED", null, bookingTime, bookingPhone);
		}

		public int getNumber() { return number; }
		public String getLabel() { return label; }
		public boolean isOccupied() { return occupied; }
		public boolean isReserved() { return reserved; }
		public String getOrderCode() { return orderCode; }
		public String getCustomerName() { return customerName; }
		public String getStatusLabel() { return statusLabel; }
		public LocalDateTime getCreatedAt() { return createdAt; }
		public LocalDateTime getBookingTime() { return bookingTime; }
		public String getBookingPhone() { return bookingPhone; }
	}
}
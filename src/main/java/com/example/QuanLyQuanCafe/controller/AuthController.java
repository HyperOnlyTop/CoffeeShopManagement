package com.example.QuanLyQuanCafe.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.Customer;
import com.example.QuanLyQuanCafe.model.CustomerReview;
import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.repository.CustomerRepository;
import com.example.QuanLyQuanCafe.repository.CustomerReviewRepository;
import com.example.QuanLyQuanCafe.service.MenuService;

@Controller
public class AuthController {

    private final MenuService menuService;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepository;
    private final CustomerReviewRepository customerReviewRepository;

    public AuthController(MenuService menuService,
                          AppUserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          CustomerRepository customerRepository,
                          CustomerReviewRepository customerReviewRepository) {
        this.menuService = menuService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerRepository = customerRepository;
        this.customerReviewRepository = customerReviewRepository;
    }

    @GetMapping("/")
    public String landingPage(Model model, Principal principal) {
        List<MenuItem> bestSellerItems = menuService.getBestSellerAvailableItems(12);
        model.addAttribute("menuItems", bestSellerItems);
        List<CustomerReview> customerReviews = customerReviewRepository.findTop3ByOrderByCreatedAtDesc();
        model.addAttribute("customerReviews", customerReviews);

        AppUser loggedInUser = null;
        if (principal != null) {
            loggedInUser = userRepository.findByUsername(principal.getName());
            model.addAttribute("currentUser", loggedInUser);
        }

        Integer landingLoyaltyPoints = null;
        if (loggedInUser != null && loggedInUser.getPhone() != null && !loggedInUser.getPhone().isBlank()) {
            Optional<Customer> cust = customerRepository.findByPhone(loggedInUser.getPhone().trim());
            if (cust.isPresent()) {
                Integer p = cust.get().getLoyaltyPoints();
                landingLoyaltyPoints = p != null ? p : 0;
            }
        }
        model.addAttribute("landingLoyaltyPoints", landingLoyaltyPoints);

        return "public/index";
    }

    @GetMapping("/menu")
    public String publicMenu(Model model,
                             @RequestParam(name = "page", defaultValue = "0") int page,
                             @RequestParam(name = "category", required = false) String category) {
        int pageSize = 8;
        if (page < 0) {
            page = 0;
        }

        String selectedCategory = (category != null && !category.isBlank()) ? category.trim() : null;
        Page<MenuItem> menuPage = (selectedCategory == null)
                ? menuService.getAvailableItemsPage(PageRequest.of(page, pageSize))
                : menuService.getAvailableItemsPageByCategory(selectedCategory, PageRequest.of(page, pageSize));

        model.addAttribute("menuPage", menuPage);
        model.addAttribute("menuItems", menuPage.getContent()); // giữ lại để tương thích nếu cần
        List<String> menuCategories = menuService.getAvailableItems().stream()
                .map(mi -> mi != null && mi.getCategory() != null ? mi.getCategory().getName() : null)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        model.addAttribute("menuCategories", menuCategories);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", menuPage.getTotalPages());

        return "public/public-menu";
    }

    @GetMapping("/reviews")
    public String publicReviews(Model model,
                                @RequestParam(name = "page", defaultValue = "0") int page) {
        int pageSize = 12;
        if (page < 0) {
            page = 0;
        }

        Page<CustomerReview> reviewsPage = customerReviewRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, pageSize));

        model.addAttribute("reviewsPage", reviewsPage);
        model.addAttribute("customerReviews", reviewsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reviewsPage.getTotalPages());

        return "public/reviews";
    }

    @GetMapping("/login")
    public String loginPage(Principal principal) {
        if (principal != null) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {
        
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu nhập lại không khớp.");
            return "auth/register";
        }
        
        if (userRepository.findByUsername(username) != null) {
            model.addAttribute("error", "Tên đăng nhập đã tồn tại.");
            return "auth/register";
        }

        String phoneTrim = phone == null ? "" : phone.trim();
        if (phoneTrim.isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập số điện thoại.");
            return "auth/register";
        }
        if (userRepository.existsByPhone(phoneTrim)) {
            model.addAttribute("error", "Số điện thoại này đã có tài khoản đăng nhập.");
            return "auth/register";
        }

        AppUser user = new AppUser();
        user.setFullName(fullName != null ? fullName.trim() : "");
        user.setEmail(email != null ? email.trim() : "");
        user.setPhone(phoneTrim);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("CUSTOMER");
        userRepository.save(user);

        Optional<Customer> existing = customerRepository.findByPhone(phoneTrim);
        if (existing.isPresent()) {
            Customer c = existing.get();
            if (fullName != null && !fullName.trim().isEmpty()) {
                c.setName(fullName.trim());
            }
            if (email != null && !email.trim().isEmpty()) {
                c.setEmail(email.trim());
            }
            customerRepository.save(c);
        } else {
            Customer customer = new Customer();
            customer.setName(fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : "Khách mới");
            customer.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);
            customer.setPhone(phoneTrim);
            customer.setTotalOrders(0);
            customer.setTotalSpent(BigDecimal.ZERO);
            customer.setLoyaltyPoints(0);
            customer.setLoyaltyRedeemedCount(0);
            customerRepository.save(customer);
        }

        return "redirect:/login?registered";
    }

    @PostMapping("/reviews")
    public String submitReview(@RequestParam(value = "name", required = false) String name,
                               @RequestParam(value = "role", required = false) String role,
                               @RequestParam(value = "phone", required = false) String phone,
                               @RequestParam("rating") int rating,
                               @RequestParam("comment") String comment,
                               Principal principal) {

        AppUser current = null;
        if (principal != null) {
            current = userRepository.findByUsername(principal.getName());
        }

        boolean loggedIn = (current != null);

        if (!loggedIn) {
            if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
                return "redirect:/?reviewError=missingInfo#testimonials";
            }
        }

        if (loggedIn) {
            name = (current.getFullName() != null && !current.getFullName().isBlank())
                    ? current.getFullName()
                    : (name != null ? name.trim() : "Khách hàng");
            if (current.getPhone() != null && !current.getPhone().isBlank()) {
                phone = current.getPhone();
            }
        }

        if (rating < 1) {
            rating = 1;
        } else if (rating > 5) {
            rating = 5;
        }

        CustomerReview review = new CustomerReview();
        review.setCustomerName(name != null ? name.trim() : "Khách hàng");
        review.setCustomerRole((role == null || role.isBlank()) ? "Khách hàng" : role.trim());
        review.setRating(rating);
        review.setComment(comment.trim());
        if (phone != null && !phone.isBlank()) {
            review.setPhone(phone.trim());
        }
        customerReviewRepository.save(review);

        return "redirect:/?reviewSuccess=1#testimonials";
    }
}

package com.example.QuanLyQuanCafe.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

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
    public String landingPage(Model model) {
        List<MenuItem> menuItems = menuService.getAllItems();
        model.addAttribute("menuItems", menuItems);
        List<CustomerReview> customerReviews = customerReviewRepository.findTop3ByOrderByCreatedAtDesc();
        model.addAttribute("customerReviews", customerReviews);
        return "index"; // trang giới thiệu công khai
    }

    @GetMapping("/menu")
    public String publicMenu(Model model,
                             @RequestParam(name = "page", defaultValue = "0") int page) {
        int pageSize = 8;
        if (page < 0) {
            page = 0;
        }

        Page<MenuItem> menuPage = menuService.getItemsPage(PageRequest.of(page, pageSize));

        model.addAttribute("menuPage", menuPage);
        model.addAttribute("menuItems", menuPage.getContent()); // giữ lại để tương thích nếu cần
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", menuPage.getTotalPages());

        return "public-menu"; // trang xem toàn bộ thực đơn cho khách
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // templates/login.html
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register"; // templates/register.html
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
            return "register";
        }
        
        if (userRepository.findByUsername(username) != null) {
            model.addAttribute("error", "Tên đăng nhập đã tồn tại.");
            return "register";
        }

        if (customerRepository.findByPhone(phone).isPresent()) {
            model.addAttribute("error", "Số điện thoại đã được đăng ký.");
            return "register";
        }
        
        AppUser user = new AppUser();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("KHACH");
        userRepository.save(user);
        
        // Save to customers table
        Customer customer = new Customer();
        customer.setName(fullName);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setTotalOrders(0);
        customer.setTotalSpent(BigDecimal.ZERO);
        customerRepository.save(customer);
        
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

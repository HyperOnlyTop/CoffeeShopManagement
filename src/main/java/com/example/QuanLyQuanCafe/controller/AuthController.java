package com.example.QuanLyQuanCafe.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.service.MenuService;

@Controller
public class AuthController {

    private final MenuService menuService;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(MenuService menuService, AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.menuService = menuService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public String landingPage(Model model) {
        List<MenuItem> menuItems = menuService.getAllItems();
        model.addAttribute("menuItems", menuItems);
        return "index"; // trang giới thiệu công khai
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
        
        AppUser user = new AppUser();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("STAFF");
        userRepository.save(user);
        
        return "redirect:/login?registered";
    }
}

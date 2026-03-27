package com.example.QuanLyQuanCafe.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.service.MenuService;

@Controller
public class AuthController {

    private final MenuService menuService;

    public AuthController(MenuService menuService) {
        this.menuService = menuService;
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
}

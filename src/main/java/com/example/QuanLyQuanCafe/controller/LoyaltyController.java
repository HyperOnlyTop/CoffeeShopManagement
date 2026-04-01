package com.example.QuanLyQuanCafe.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.controller.dto.LoyaltyAdjustRequest;
import com.example.QuanLyQuanCafe.controller.dto.LoyaltyRedeemRequest;
import com.example.QuanLyQuanCafe.model.Customer;
import com.example.QuanLyQuanCafe.repository.CustomerRepository;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final CustomerRepository customerRepository;

    public LoyaltyController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping("/customers")
    public ResponseEntity<List<Customer>> getCustomers() {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Customer> customers = customerRepository.findAll();
        customers.sort(Comparator.comparing(Customer::getId, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return ResponseEntity.ok(customers);
    }

    @PostMapping("/redeem")
    public ResponseEntity<?> redeem(@RequestBody LoyaltyRedeemRequest request) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String phone = request != null ? request.getPhone() : null;
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body("Số điện thoại không được để trống");
        }

        Customer customer = customerRepository.findByPhone(phone.trim()).orElse(null);
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy khách hàng theo SĐT");
        }

        int points = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        if (points < 10) {
            return ResponseEntity.badRequest().body("Không đủ điểm để đổi (cần 10 điểm)");
        }

        customer.setLoyaltyPoints(points - 10);
        int redeemed = customer.getLoyaltyRedeemedCount() != null ? customer.getLoyaltyRedeemedCount() : 0;
        customer.setLoyaltyRedeemedCount(redeemed + 1);
        return ResponseEntity.ok(customerRepository.save(customer));
    }

    @PostMapping("/adjust")
    public ResponseEntity<?> adjust(@RequestBody LoyaltyAdjustRequest request) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String phone = request != null ? request.getPhone() : null;
        Integer delta = request != null ? request.getDelta() : null;
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body("Số điện thoại không được để trống");
        }
        if (delta == null) {
            return ResponseEntity.badRequest().body("Delta không được để trống");
        }

        Customer customer = customerRepository.findByPhone(phone.trim()).orElse(null);
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy khách hàng theo SĐT");
        }

        int points = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        int next = Math.max(0, points + delta);
        customer.setLoyaltyPoints(next);
        return ResponseEntity.ok(customerRepository.save(customer));
    }
}


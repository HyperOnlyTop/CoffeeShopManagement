package com.example.QuanLyQuanCafe.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.controller.dto.UserCreateRequest;
import com.example.QuanLyQuanCafe.controller.dto.UserLinkStaffRequest;
import com.example.QuanLyQuanCafe.controller.dto.UserUpdateRequest;
import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.repository.StaffRepository;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StaffRepository staffRepository;

    public UserAdminController(AppUserRepository userRepository, PasswordEncoder passwordEncoder, StaffRepository staffRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.staffRepository = staffRepository;
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    public ResponseEntity<List<AppUser>> getAllUsers() {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserCreateRequest request) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body("Username không được để trống");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Mật khẩu không được để trống");
        }

        if (userRepository.findByUsername(request.getUsername()) != null) {
            return ResponseEntity.badRequest().body("Username đã tồn tại");
        }

        Long staffId = request.getStaffId();
        if (staffId == null) {
            return ResponseEntity.badRequest().body("Vui lòng chọn nhân viên để gán cho tài khoản");
        }
        if (staffRepository.findById(staffId).isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy nhân viên");
        }
        AppUser other = userRepository.findByStaffId(staffId);
        if (other != null) {
            return ResponseEntity.badRequest().body("Nhân viên này đã được gán cho tài khoản: " + other.getUsername());
        }

        AppUser user = new AppUser();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setStaffId(staffId);

        AppUser saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") Long id, @RequestBody UserUpdateRequest request) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        AppUser user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy tài khoản");
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        AppUser saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        AppUser user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy tài khoản");
        }

        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/staff")
    public ResponseEntity<?> linkStaff(@PathVariable("id") Long id, @RequestBody UserLinkStaffRequest request) {
        if (!isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        AppUser user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy tài khoản");
        }

        Long staffId = request != null ? request.getStaffId() : null;
        if (staffId == null) {
            user.setStaffId(null);
            return ResponseEntity.ok(userRepository.save(user));
        }

        if (staffRepository.findById(staffId).isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy nhân viên");
        }

        AppUser other = userRepository.findByStaffId(staffId);
        if (other != null && other.getId() != null && !other.getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body("Nhân viên này đã được gán cho tài khoản: " + other.getUsername());
        }

        user.setStaffId(staffId);
        AppUser saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
    }
}

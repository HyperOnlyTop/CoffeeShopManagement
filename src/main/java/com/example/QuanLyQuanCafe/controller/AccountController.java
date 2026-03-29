package com.example.QuanLyQuanCafe.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.repository.StaffRepository;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StaffRepository staffRepository;

    public AccountController(AppUserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             StaffRepository staffRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.staffRepository = staffRepository;
    }

    private void syncStaffAvatar(AppUser user) {
        if (user == null || user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            return;
        }

        Staff staff = null;

        // Ưu tiên ghép theo số điện thoại nếu có
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            staff = staffRepository.findByPhone(user.getPhone());
        }

        // Nếu không có hoặc không tìm được theo phone thì thử theo tên đầy đủ
        if (staff == null && user.getFullName() != null && !user.getFullName().isBlank()) {
            staff = staffRepository.findByName(user.getFullName());
        }

        // Cuối cùng fallback theo username (trường hợp tên trùng username)
        if (staff == null) {
            staff = staffRepository.findByName(user.getUsername());
        }

        if (staff != null) {
            staff.setAvatarUrl(user.getAvatarUrl());
            staffRepository.save(staff);
        }
    }

    @PostMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        
        AppUser user = userRepository.findByUsername(principal.getName());
        if (user == null) return ResponseEntity.status(404).body("User not found");

        user.setFullName(request.get("fullName"));
        user.setEmail(request.get("email"));
        user.setPhone(request.get("phone"));
        if (request.containsKey("avatarUrl")) {
            user.setAvatarUrl(request.get("avatarUrl"));
        }
        userRepository.save(user);
        syncStaffAvatar(user);

        return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin thành công"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        
        AppUser user = userRepository.findByUsername(principal.getName());
        if (user == null) return ResponseEntity.status(404).body("User not found");

        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        String confirmPassword = request.get("confirmPassword");

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mật khẩu hiện tại không đúng"));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mật khẩu xác nhận không khớp"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng chọn file"));

        ResponseEntity<?> response;
        try {
            String originalName = file.getOriginalFilename();
            String extension = ".png"; // default
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }

            String uploadDir = "uploads/avatars";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            String avatarUrl = "/uploads/avatars/" + fileName;
            
            AppUser user = userRepository.findByUsername(principal.getName());
            if (user != null) {
                user.setAvatarUrl(avatarUrl);
                userRepository.save(user);
                syncStaffAvatar(user);
            }

            response = ResponseEntity.ok(Map.of("avatarUrl", avatarUrl, "message", "Tải ảnh lên thành công"));
        } catch (IOException e) {
            response = ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lưu file: " + e.getMessage()));
        }
        return response;
    }
}

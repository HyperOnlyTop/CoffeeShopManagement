package com.example.QuanLyQuanCafe.controller;

import java.security.Principal;
import java.util.HashMap;
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
import com.example.QuanLyQuanCafe.repository.AppUserRepository;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountController(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
            }

            response = ResponseEntity.ok(Map.of("avatarUrl", avatarUrl, "message", "Tải ảnh lên thành công"));
        } catch (IOException e) {
            response = ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lưu file: " + e.getMessage()));
        }
        return response;
    }
}

package com.example.QuanLyQuanCafe.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;

@Component
public class DatabaseInitializer {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (userRepository.findByUsername("admin") == null) {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setFullName("Quản trị viên");
            admin.setEmail("admin@cafe.com");
            admin.setPhone("0901 234 567");
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }
        if (userRepository.findByUsername("nhanvien") == null) {
            AppUser staff = new AppUser();
            staff.setUsername("nhanvien");
            staff.setPassword(passwordEncoder.encode("123456"));
            staff.setFullName("Nhân viên thu ngân");
            staff.setEmail("nhanvien@cafe.com");
            staff.setPhone("0912 345 678");
            staff.setRole("STAFF");
            userRepository.save(staff);
        }
    }
}

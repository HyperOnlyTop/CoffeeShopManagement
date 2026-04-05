package com.example.QuanLyQuanCafe.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private final AppUserRepository userRepository;

    public CustomUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = userRepository.findByUsername(username);
        if (appUser == null) {
            throw new UsernameNotFoundException("Không tìm thấy người dùng: " + username);
        }
        if (!appUser.isEnabled()) {
            throw new UsernameNotFoundException("Tài khoản đã bị vô hiệu hoá.");
        }
        String role = appUser.getRole();
        if (role == null || role.isBlank()) {
            role = "CUSTOMER";
        } else if ("KHACH".equalsIgnoreCase(role)) {
            role = "CUSTOMER";
        }
        return User.builder()
                .username(appUser.getUsername())
                .password(appUser.getPassword())
                .roles(role)
                .build();
    }
}

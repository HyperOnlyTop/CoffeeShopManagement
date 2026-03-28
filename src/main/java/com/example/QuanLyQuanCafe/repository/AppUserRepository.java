package com.example.QuanLyQuanCafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.QuanLyQuanCafe.model.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    AppUser findByUsername(String username);
}

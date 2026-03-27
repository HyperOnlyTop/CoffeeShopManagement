package com.example.QuanLyQuanCafe.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuanLyQuanCafe.model.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

	Optional<MenuItem> findByName(String name);
}

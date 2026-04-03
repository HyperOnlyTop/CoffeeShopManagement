package com.example.QuanLyQuanCafe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuanLyQuanCafe.model.InventoryCategory;

public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, Long> {

	Optional<InventoryCategory> findByName(String name);

	List<InventoryCategory> findAllByOrderByNameAsc();
}

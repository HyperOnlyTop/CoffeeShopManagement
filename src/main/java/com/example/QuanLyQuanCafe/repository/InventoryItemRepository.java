package com.example.QuanLyQuanCafe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuanLyQuanCafe.model.InventoryItem;
import com.example.QuanLyQuanCafe.model.InventoryStatus;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByStatus(InventoryStatus status);

    Optional<InventoryItem> findByName(String name);
}

package com.example.QuanLyQuanCafe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.model.MenuItemStatus;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

	Optional<MenuItem> findByName(String name);

	List<MenuItem> findByStatus(MenuItemStatus status);

	Page<MenuItem> findByStatus(MenuItemStatus status, Pageable pageable);

	Page<MenuItem> findByStatusAndCategory_Name(MenuItemStatus status, String categoryName, Pageable pageable);

    List<MenuItem> findTop20ByStatusAndNameContainingIgnoreCaseOrderByNameAsc(MenuItemStatus status, String name);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE menu_items SET created_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE created_at IS NULL", nativeQuery = true)
    int backfillNullTimestamps();
}

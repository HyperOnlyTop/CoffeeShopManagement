package com.example.QuanLyQuanCafe.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuanLyQuanCafe.model.DailyRevenue;

public interface DailyRevenueRepository extends JpaRepository<DailyRevenue, LocalDate> {
}

package com.example.QuanLyQuanCafe.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.QuanLyQuanCafe.model.DailyRevenue;
import com.example.QuanLyQuanCafe.repository.DailyRevenueRepository;

@Service
public class DailyRevenueService {

    private final DailyRevenueRepository dailyRevenueRepository;

    public DailyRevenueService(DailyRevenueRepository dailyRevenueRepository) {
        this.dailyRevenueRepository = dailyRevenueRepository;
    }

    public List<DailyRevenue> findAll() {
        return dailyRevenueRepository.findAll();
    }

    public DailyRevenue findByDate(LocalDate date) {
        return dailyRevenueRepository.findById(date).orElse(null);
    }

    public DailyRevenue save(DailyRevenue dailyRevenue) {
        return dailyRevenueRepository.save(dailyRevenue);
    }
}

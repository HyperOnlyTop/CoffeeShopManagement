package com.example.QuanLyQuanCafe.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuanLyQuanCafe.model.CustomerReview;

public interface CustomerReviewRepository extends JpaRepository<CustomerReview, Long> {

    List<CustomerReview> findTop3ByOrderByCreatedAtDesc();

    Page<CustomerReview> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

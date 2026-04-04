package com.example.QuanLyQuanCafe.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.OrderStatus;

public interface CafeOrderRepository extends JpaRepository<CafeOrder, Long> {

    CafeOrder findByOrderCode(String orderCode);

    List<CafeOrder> findByStatus(OrderStatus status);

    List<CafeOrder> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<CafeOrder> findTop30ByStatusAndPreparedAtAfterOrderByPreparedAtAsc(OrderStatus status, LocalDateTime after);
}

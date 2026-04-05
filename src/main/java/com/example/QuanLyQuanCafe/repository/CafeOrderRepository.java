package com.example.QuanLyQuanCafe.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.OrderStatus;

public interface CafeOrderRepository extends JpaRepository<CafeOrder, Long> {

    CafeOrder findByOrderCode(String orderCode);

    /** Mã đơn dạng {@code ORD-yymmdd-NNNN} — lấy mã lớn nhất trong ngày (suffix zero-pad 4 chữ số). */
    Optional<CafeOrder> findFirstByOrderCodeStartingWithOrderByOrderCodeDesc(String prefix);

    List<CafeOrder> findByStatus(OrderStatus status);

    List<CafeOrder> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<CafeOrder> findTop30ByStatusAndPreparedAtAfterOrderByPreparedAtAsc(OrderStatus status, LocalDateTime after);
}

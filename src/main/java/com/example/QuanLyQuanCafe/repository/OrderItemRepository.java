package com.example.QuanLyQuanCafe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.QuanLyQuanCafe.model.CafeOrder;
import com.example.QuanLyQuanCafe.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(CafeOrder order);

    @Query("""
            select oi.menuItem.id, sum(coalesce(oi.quantity, 0))
            from OrderItem oi
            where oi.menuItem is not null
              and oi.order is not null
              and oi.order.status = com.example.QuanLyQuanCafe.model.OrderStatus.COMPLETED
            group by oi.menuItem.id
            order by sum(coalesce(oi.quantity, 0)) desc
            """)
    List<Object[]> findBestSellerMenuItemIds();
}

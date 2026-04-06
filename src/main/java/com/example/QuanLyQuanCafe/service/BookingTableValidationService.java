package com.example.QuanLyQuanCafe.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.QuanLyQuanCafe.model.BookingStatus;
import com.example.QuanLyQuanCafe.model.TableBooking;
import com.example.QuanLyQuanCafe.repository.TableBookingRepository;

@Service
public class BookingTableValidationService {

    private final OrderService orderService;
    private final TableBookingRepository tableBookingRepository;

    public BookingTableValidationService(OrderService orderService, TableBookingRepository tableBookingRepository) {
        this.orderService = orderService;
        this.tableBookingRepository = tableBookingRepository;
    }

    /**
     * Kiểm tra gán {@code reservedTableNumber} khi lưu đặt bàn.
     * Logic mới: chỉ check bàn có đang có đơn hoặc đã có booking CONFIRMED khác gán.
     */
    public Optional<String> validateReservedTableForBooking(Integer tableNo, LocalDateTime bookingTime, Long excludeBookingId) {
        if (tableNo == null || tableNo < 1) {
            return Optional.empty();
        }
        if (bookingTime == null) {
            return Optional.of("Vui lòng chọn thời gian đặt bàn trước khi gán giữ bàn.");
        }

        // Check bàn đang có đơn
        if (orderService.findActiveDineInOrderForTable(tableNo) != null) {
            return Optional.of("Bàn " + tableNo + " đang có khách (có đơn tại bàn). Chọn bàn khác hoặc dùng Trả bàn ở Quản lý bàn.");
        }

        // Check bàn đã có booking CONFIRMED khác gán
        List<TableBooking> sameTable = tableBookingRepository.findByReservedTableNumberAndStatus(tableNo, BookingStatus.CONFIRMED);
        for (TableBooking other : sameTable) {
            if (excludeBookingId != null && other.getId() != null && other.getId().equals(excludeBookingId)) {
                continue;
            }
            return Optional.of("Bàn " + tableNo + " đã được giữ bởi booking khác (" + other.getName() + "). Bỏ gán bàn đó trước hoặc chọn bàn khác.");
        }
        return Optional.empty();
    }
}

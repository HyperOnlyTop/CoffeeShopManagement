package com.example.QuanLyQuanCafe.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.QuanLyQuanCafe.config.BookingPolicy;
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
     * Kiểm tra gán {@code reservedTableNumber} khi lưu đặt bàn: không trùng đơn tại bàn, không trùng cửa sổ giữ bàn với đặt khác.
     */
    public Optional<String> validateReservedTableForBooking(Integer tableNo, LocalDateTime bookingTime, Long excludeBookingId) {
        if (tableNo == null || tableNo < 1) {
            return Optional.empty();
        }
        if (bookingTime == null) {
            return Optional.of("Vui lòng chọn thời gian đặt bàn trước khi gán giữ bàn.");
        }

        if (orderService.findActiveDineInOrderForTable(tableNo) != null) {
            return Optional.of("Bàn " + tableNo + " đang có khách (có đơn tại bàn). Chọn bàn khác hoặc dùng Trả bàn ở Quản lý bàn.");
        }

        LocalDateTime winStart = bookingTime.minusMinutes(BookingPolicy.HOLD_BEFORE_MINUTES);
        LocalDateTime winEnd = bookingTime.plusMinutes(BookingPolicy.GRACE_AFTER_MINUTES);

        List<TableBooking> sameTable = tableBookingRepository.findByReservedTableNumberAndStatus(tableNo, BookingStatus.CONFIRMED);
        for (TableBooking other : sameTable) {
            if (excludeBookingId != null && other.getId() != null && other.getId().equals(excludeBookingId)) {
                continue;
            }
            if (other.getBookingTime() == null) {
                continue;
            }
            LocalDateTime oStart = other.getBookingTime().minusMinutes(BookingPolicy.HOLD_BEFORE_MINUTES);
            LocalDateTime oEnd = other.getBookingTime().plusMinutes(BookingPolicy.GRACE_AFTER_MINUTES);
            if (!winEnd.isBefore(oStart) && !winStart.isAfter(oEnd)) {
                return Optional.of("Bàn " + tableNo + " đã được giữ bởi đặt bàn khác trong khoảng thời gian trùng (cửa sổ ±15 phút).");
            }
        }
        return Optional.empty();
    }
}

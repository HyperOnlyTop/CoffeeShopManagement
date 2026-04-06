package com.example.QuanLyQuanCafe.model;

/** Loại nhắc nội bộ cho nhân viên (lịch sử lưu trong DB). */
public enum BookingReminderKind {
    /** Trước 15 phút tới giờ khách hẹn — gợi ý gán bàn / chuẩn bị. */
    FIFTEEN_MIN_BEFORE,

    /** Đúng giờ đặt bàn — nhắc nhân viên khách sắp đến. */
    AT_BOOKING_TIME,

    /** Quá 15 phút sau giờ đặt mà chưa tick đã đến hoặc hủy — nhắc xử lý. */
    OVERDUE_15MIN
}

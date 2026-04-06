package com.example.QuanLyQuanCafe.config;

public final class BookingPolicy {
    private BookingPolicy() {}

    /** Thời điểm đến phải cách lúc đặt ít nhất bấy nhiêu giờ - dành cho khách đặt qua website. */
    public static final int MIN_LEAD_HOURS = 2;

    /** Thời điểm đến phải cách hiện tại ít nhất bấy nhiêu phút - dành cho nhân viên tạo đặt bàn. */
    public static final int MIN_LEAD_MINUTES_STAFF = 20;

    /** Nhắc nhở trước giờ đặt bao nhiêu phút. */
    public static final int REMINDER_BEFORE_MINUTES = 15;

    /** Nhắc nhở sau giờ đặt bao nhiêu phút nếu chưa xử lý. */
    public static final int REMINDER_OVERDUE_MINUTES = 15;
}

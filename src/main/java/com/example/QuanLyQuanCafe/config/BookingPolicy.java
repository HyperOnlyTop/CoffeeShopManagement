package com.example.QuanLyQuanCafe.config;

public final class BookingPolicy {
    private BookingPolicy() {}

    public static final int HOLD_BEFORE_MINUTES = 15;
    public static final int GRACE_AFTER_MINUTES = 15;

    /** Thời điểm đến phải cách lúc đặt ít nhất bấy nhiêu giờ (tránh đặt sát giờ quán không kịp xử lý). */
    public static final int MIN_LEAD_HOURS = 2;
}

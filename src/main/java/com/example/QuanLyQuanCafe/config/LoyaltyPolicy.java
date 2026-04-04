package com.example.QuanLyQuanCafe.config;

import java.math.BigDecimal;

/** Đồng bộ với Quản lý khách hàng / Accounts: 10 điểm = 1 ly, món &lt; 50k. */
public final class LoyaltyPolicy {

    private LoyaltyPolicy() {}

    public static final int POINTS_PER_FREE_DRINK = 10;

    public static final BigDecimal MAX_MENU_PRICE_FREE_DRINK_VND = new BigDecimal("50000");
}

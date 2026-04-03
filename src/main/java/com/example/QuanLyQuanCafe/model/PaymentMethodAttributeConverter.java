package com.example.QuanLyQuanCafe.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Đọc giá trị payment_method cũ không còn trong enum (ví dụ đã xóa khỏi hệ thống)
 * thành {@link PaymentMethod#BANK_TRANSFER} để tránh lỗi khi load entity.
 */
@Converter(autoApply = false)
public class PaymentMethodAttributeConverter implements AttributeConverter<PaymentMethod, String> {

    @Override
    public String convertToDatabaseColumn(PaymentMethod attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PaymentMethod convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        String v = dbData.trim();
        try {
            return PaymentMethod.valueOf(v);
        } catch (IllegalArgumentException ex) {
            return PaymentMethod.BANK_TRANSFER;
        }
    }
}

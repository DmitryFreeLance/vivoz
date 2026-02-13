package ru.vivoz.bot;

public record OrderSummary(
        long id,
        String orderType,
        String dateValue,
        String phone,
        String createdAt
) {
}

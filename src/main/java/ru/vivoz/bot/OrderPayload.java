package ru.vivoz.bot;

import java.util.Map;

public record OrderPayload(
        long userId,
        String username,
        String fullName,
        OrderType type,
        String createdAt,
        String dateValue,
        String phone,
        Map<String, String> answers
) {
}

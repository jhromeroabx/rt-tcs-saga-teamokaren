package org.teamokaren.infrastructure.adapter.out.http;

public record OrderServiceCreateRequest(
        String orderId,
        String customerId,
        String currency,
        Double totalAmount) {
}
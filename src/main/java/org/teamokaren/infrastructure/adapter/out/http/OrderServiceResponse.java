package org.teamokaren.infrastructure.adapter.out.http;

public record OrderServiceResponse(
        String orderId,
        String status,
        String message) {
}
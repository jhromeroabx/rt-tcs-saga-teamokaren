package org.teamokaren.infrastructure.adapter.out.http;

public record OrderServiceCancelRequest(
        String reason,
        String requestedBy) {
}
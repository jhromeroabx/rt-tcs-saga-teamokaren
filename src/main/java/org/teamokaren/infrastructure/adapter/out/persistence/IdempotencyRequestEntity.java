package org.teamokaren.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "idempotency_request")
@Getter
@Setter
public class IdempotencyRequestEntity {
    @Id
    @Column(name = "request_id")
    private String requestId;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "response_body", columnDefinition = "CLOB")
    private String responseBody;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}

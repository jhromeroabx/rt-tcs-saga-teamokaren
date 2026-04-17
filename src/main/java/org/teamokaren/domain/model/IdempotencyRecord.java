package org.teamokaren.domain.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {
    private String requestId;
    private String resourceType;
    private String requestHash;
    private UUID paymentId;
    private String responseBody;
    private Integer httpStatus;
    private Instant createdAt;
    private Instant expiresAt;
}

package org.teamokaren.infrastructure.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.teamokaren.domain.model.IdempotencyRecord;
import org.teamokaren.domain.port.IdempotencyPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdempotencyRepositoryAdapter implements IdempotencyPort {

    private final IdempotencyRepository repository;

    @Override
    public Optional<IdempotencyRecord> find(String requestId) {
        return repository.findById(requestId)
                .map(entity -> IdempotencyRecord.builder()
                        .requestId(entity.getRequestId())
                        .resourceType(entity.getResourceType())
                        .requestHash(entity.getRequestHash())
                        .paymentId(entity.getPaymentId() == null ? null : java.util.UUID.fromString(entity.getPaymentId()))
                        .responseBody(entity.getResponseBody())
                        .httpStatus(entity.getHttpStatus())
                        .createdAt(entity.getCreatedAt())
                        .expiresAt(entity.getExpiresAt())
                        .build());
    }

    @Override
    public void save(IdempotencyRecord record) {
        IdempotencyRequestEntity entity = new IdempotencyRequestEntity();
        entity.setRequestId(record.getRequestId());
        entity.setResourceType(record.getResourceType());
        entity.setRequestHash(record.getRequestHash());
        entity.setPaymentId(record.getPaymentId() == null ? null : record.getPaymentId().toString());
        entity.setResponseBody(record.getResponseBody());
        entity.setHttpStatus(record.getHttpStatus());
        entity.setCreatedAt(record.getCreatedAt());
        entity.setExpiresAt(record.getExpiresAt());
        repository.save(entity);
    }
}
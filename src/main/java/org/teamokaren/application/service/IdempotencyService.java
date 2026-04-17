package org.teamokaren.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.teamokaren.domain.exception.IdempotencyConflictException;
import org.teamokaren.domain.model.IdempotencyRecord;
import org.teamokaren.domain.port.PaymentSagaRepositoryPort;
import org.teamokaren.domain.port.IdempotencyPort;
import org.teamokaren.payments.api.model.PaymentSagaResponse;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyPort idempotencyPort;
    private final PaymentSagaRepositoryPort paymentSagaRepository;
    private final PaymentSagaResponseMapper paymentSagaResponseMapper;
    private final ObjectMapper objectMapper;

    public Optional<PaymentSagaResponse> findReplay(String requestId, String requestHash) {
        return idempotencyPort.find(requestId)
                .map(record -> {
                    if (!record.getRequestHash().equals(requestHash)) {
                        throw new IdempotencyConflictException();
                    }
                    try {
                        return objectMapper.readValue(record.getResponseBody(), PaymentSagaResponse.class);
                    } catch (JsonProcessingException exception) {
                        return paymentSagaRepository.findById(record.getPaymentId())
                                .map(paymentSagaResponseMapper::toResponse)
                                .orElseThrow(() -> new RuntimeException("No fue posible reconstruir la respuesta idempotente", exception));
                    }
                });
    }

    public String hash(Object payload) {
        try {
            String serialized = payload instanceof String text
                    ? text
                    : objectMapper.writeValueAsString(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(serialized.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new RuntimeException("No fue posible calcular el hash idempotente", exception);
        }
    }

    public void save(
            String requestId,
            String resourceType,
            String requestHash,
            UUID paymentId,
            PaymentSagaResponse response,
            int httpStatus) {
        try {
            idempotencyPort.save(IdempotencyRecord.builder()
                    .requestId(requestId)
                    .resourceType(resourceType)
                    .requestHash(requestHash)
                    .paymentId(paymentId)
                    .responseBody(objectMapper.writeValueAsString(response))
                    .httpStatus(httpStatus)
                    .createdAt(Instant.now())
                    .build());
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("No fue posible serializar la respuesta idempotente", exception);
        }
    }
}
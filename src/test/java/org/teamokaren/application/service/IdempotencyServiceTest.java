package org.teamokaren.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.teamokaren.domain.model.IdempotencyRecord;
import org.teamokaren.domain.model.PaymentSaga;
import org.teamokaren.domain.model.PaymentSagaStatus;
import org.teamokaren.domain.model.SagaStepName;
import org.teamokaren.domain.port.IdempotencyPort;
import org.teamokaren.domain.port.PaymentSagaRepositoryPort;
import org.teamokaren.payments.api.model.PaymentSagaResponse;

class IdempotencyServiceTest {

    @Test
    void shouldFallbackToRepositoryWhenStoredReplayHasLegacyFormat() {
        IdempotencyPort idempotencyPort = mock(IdempotencyPort.class);
        PaymentSagaRepositoryPort paymentSagaRepository = mock(PaymentSagaRepositoryPort.class);
        PaymentSagaResponseMapper mapper = new PaymentSagaResponseMapper();
        IdempotencyService service = new IdempotencyService(idempotencyPort, paymentSagaRepository, mapper, new ObjectMapper());

        UUID paymentId = UUID.randomUUID();
        String requestId = "compensation-0001";
        String requestHash = "hash-123";

        IdempotencyRecord record = IdempotencyRecord.builder()
                .requestId(requestId)
                .requestHash(requestHash)
                .paymentId(paymentId)
                .responseBody("""
                        {"paymentId":"%s","failureReason":{"present":false},"createdAt":1776400205.313459}
                        """.formatted(paymentId))
                .build();

        PaymentSaga saga = PaymentSaga.builder()
                .paymentId(paymentId)
                .sagaId(UUID.randomUUID())
                .orderId("ORD-100045")
                .customerId("CUS-9001")
                .currency("PEN")
                .totalAmount(new BigDecimal("149.90"))
                .status(PaymentSagaStatus.COMPENSATED)
                .currentStep(SagaStepName.CANCEL_ORDER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .steps(List.of())
                .build();

        when(idempotencyPort.find(requestId)).thenReturn(Optional.of(record));
        when(paymentSagaRepository.findById(paymentId)).thenReturn(Optional.of(saga));

        Optional<PaymentSagaResponse> replay = service.findReplay(requestId, requestHash);

        assertThat(replay).isPresent();
        assertThat(replay.get().getPaymentId()).isEqualTo(paymentId);
        assertThat(replay.get().getStatus().name()).isEqualTo("COMPENSATED");
    }
}
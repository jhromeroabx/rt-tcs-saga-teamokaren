package org.teamokaren.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.teamokaren.payments.api.model.PaymentSagaResponse;
import org.teamokaren.payments.api.model.PaymentSagaStatus;
import org.teamokaren.payments.api.model.SagaStepName;

@JsonTest
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeDatesAsIsoAndSkipAbsentJsonNullableFields() throws Exception {
        PaymentSagaResponse response = new PaymentSagaResponse();
        response.setPaymentId(java.util.UUID.randomUUID());
        response.setSagaId(java.util.UUID.randomUUID());
        response.setOrderId("ORD-100045");
        response.setCustomerId("CUS-9001");
        response.setCurrency("PEN");
        response.setTotalAmount(149.90);
        response.setStatus(PaymentSagaStatus.COMPLETED);
        response.setCurrentStep(SagaStepName.DELIVER_ORDER);
        response.setCreatedAt(OffsetDateTime.parse("2026-04-16T23:00:00Z"));
        response.setUpdatedAt(OffsetDateTime.parse("2026-04-16T23:01:00Z"));
        response.setFailureReason(null);
        response.setCompensationReason("rollback manual");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"createdAt\":\"2026-04-16T23:00:00Z\"");
        assertThat(json).contains("\"compensationReason\":\"rollback manual\"");
        assertThat(json).doesNotContain("present");
        assertThat(json).contains("\"failureReason\":null");
    }
}
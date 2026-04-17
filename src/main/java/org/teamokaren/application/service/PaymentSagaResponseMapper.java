package org.teamokaren.application.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;
import org.teamokaren.domain.model.PaymentSaga;
import org.teamokaren.domain.model.PaymentStep;
import org.teamokaren.payments.api.model.PaymentSagaResponse;
import org.teamokaren.payments.api.model.SagaStepStatus;

@Component
public class PaymentSagaResponseMapper {

    public PaymentSagaResponse toResponse(PaymentSaga paymentSaga) {
        PaymentSagaResponse response = new PaymentSagaResponse();
        response.setPaymentId(paymentSaga.getPaymentId());
        response.setSagaId(paymentSaga.getSagaId());
        response.setOrderId(paymentSaga.getOrderId());
        response.setCustomerId(paymentSaga.getCustomerId());
        response.setCurrency(paymentSaga.getCurrency());
        response.setTotalAmount(paymentSaga.getTotalAmount().doubleValue());
        response.setStatus(org.teamokaren.payments.api.model.PaymentSagaStatus.valueOf(paymentSaga.getStatus().name()));
        response.setCurrentStep(org.teamokaren.payments.api.model.SagaStepName.valueOf(paymentSaga.getCurrentStep().name()));
        if (paymentSaga.getFailureReason() != null) {
            response.setFailureReason(JsonNullable.of(paymentSaga.getFailureReason()));
        }
        if (paymentSaga.getCompensationReason() != null) {
            response.setCompensationReason(JsonNullable.of(paymentSaga.getCompensationReason()));
        }
        response.setCreatedAt(OffsetDateTime.ofInstant(paymentSaga.getCreatedAt(), ZoneOffset.UTC));
        response.setUpdatedAt(OffsetDateTime.ofInstant(paymentSaga.getUpdatedAt(), ZoneOffset.UTC));
        response.setSteps(paymentSaga.getSteps().stream().map(this::mapStep).toList());
        return response;
    }

    private SagaStepStatus mapStep(PaymentStep step) {
        SagaStepStatus status = new SagaStepStatus();
        status.setStep(org.teamokaren.payments.api.model.SagaStepName.valueOf(step.getStepName().name()));
        status.setService(step.getServiceName());
        status.setSequence(step.getSequence());
        status.setDirection(SagaStepStatus.DirectionEnum.fromValue(step.getDirection().name()));
        status.setStatus(org.teamokaren.payments.api.model.SagaStepExecutionStatus.fromValue(step.getStatus().name()));
        if (step.getStartedAt() != null) {
            status.setStartedAt(JsonNullable.of(OffsetDateTime.ofInstant(step.getStartedAt(), ZoneOffset.UTC)));
        }
        if (step.getFinishedAt() != null) {
            status.setFinishedAt(JsonNullable.of(OffsetDateTime.ofInstant(step.getFinishedAt(), ZoneOffset.UTC)));
        }
        if (step.getErrorCode() != null) {
            status.setErrorCode(JsonNullable.of(step.getErrorCode()));
        }
        if (step.getErrorMessage() != null) {
            status.setErrorMessage(JsonNullable.of(step.getErrorMessage()));
        }
        return status;
    }
}

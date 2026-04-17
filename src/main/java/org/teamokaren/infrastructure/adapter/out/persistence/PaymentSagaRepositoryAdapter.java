package org.teamokaren.infrastructure.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.teamokaren.domain.model.PaymentItem;
import org.teamokaren.domain.model.PaymentMethod;
import org.teamokaren.domain.model.PaymentSaga;
import org.teamokaren.domain.model.PaymentSagaStatus;
import org.teamokaren.domain.model.PaymentStep;
import org.teamokaren.domain.model.SagaStepDirection;
import org.teamokaren.domain.model.SagaStepExecutionStatus;
import org.teamokaren.domain.model.SagaStepName;
import org.teamokaren.domain.model.ShippingAddress;
import org.teamokaren.domain.port.PaymentSagaRepositoryPort;

@Component
@RequiredArgsConstructor
public class PaymentSagaRepositoryAdapter implements PaymentSagaRepositoryPort {

    private final PaymentSagaRepository paymentSagaRepository;

    @Override
    public PaymentSaga save(PaymentSaga paymentSaga) {
        PaymentSagaEntity entity = paymentSagaRepository.findById(paymentSaga.getPaymentId().toString())
                .orElseGet(PaymentSagaEntity::new);

        mapScalarFields(entity, paymentSaga);
        syncItems(entity, paymentSaga);
        syncSteps(entity, paymentSaga);

        return mapToDomain(paymentSagaRepository.save(entity));
    }

    @Override
    public Optional<PaymentSaga> findById(UUID paymentId) {
        return paymentSagaRepository.findById(paymentId.toString())
                .map(this::mapToDomain);
    }

    private void mapScalarFields(PaymentSagaEntity entity, PaymentSaga paymentSaga) {
        entity.setPaymentId(paymentSaga.getPaymentId().toString());
        entity.setSagaId(paymentSaga.getSagaId().toString());
        entity.setOrderId(paymentSaga.getOrderId());
        entity.setCustomerId(paymentSaga.getCustomerId());
        entity.setCurrency(paymentSaga.getCurrency());
        entity.setTotalAmount(paymentSaga.getTotalAmount());
        entity.setPaymentMethodType(paymentSaga.getPaymentMethod().getType());
        entity.setPaymentToken(paymentSaga.getPaymentMethod().getToken());
        entity.setCardLast4(paymentSaga.getPaymentMethod().getCardLast4());
        entity.setInstallments(paymentSaga.getPaymentMethod().getInstallments());
        entity.setStatus(paymentSaga.getStatus().name());
        entity.setCurrentStep(paymentSaga.getCurrentStep().name());
        entity.setFailureReason(paymentSaga.getFailureReason());
        entity.setCompensationReason(paymentSaga.getCompensationReason());
        entity.setShippingStreet(paymentSaga.getShippingAddress().getStreet());
        entity.setShippingCity(paymentSaga.getShippingAddress().getCity());
        entity.setShippingState(paymentSaga.getShippingAddress().getState());
        entity.setShippingCountry(paymentSaga.getShippingAddress().getCountry());
        entity.setShippingPostalCode(paymentSaga.getShippingAddress().getPostalCode());
        entity.setShippingReference(paymentSaga.getShippingAddress().getReference());
        entity.setCreatedAt(paymentSaga.getCreatedAt());
        entity.setUpdatedAt(paymentSaga.getUpdatedAt());
    }

    private void syncItems(PaymentSagaEntity entity, PaymentSaga paymentSaga) {
        entity.getItems().clear();
        for (PaymentItem item : paymentSaga.getItems()) {
            PaymentItemEntity itemEntity = new PaymentItemEntity();
            itemEntity.setPaymentSaga(entity);
            itemEntity.setSku(item.getSku());
            itemEntity.setName(item.getName());
            itemEntity.setQuantity(item.getQuantity());
            itemEntity.setUnitPrice(item.getUnitPrice());
            entity.getItems().add(itemEntity);
        }
    }

    private void syncSteps(PaymentSagaEntity entity, PaymentSaga paymentSaga) {
        Map<String, PaymentSagaStepEntity> existingSteps = entity.getSteps().stream()
                .collect(Collectors.toMap(this::stepKey, Function.identity(), (left, right) -> left));

        List<PaymentSagaStepEntity> updatedSteps = new ArrayList<>();
        for (PaymentStep step : paymentSaga.getSteps()) {
            String key = step.getStepName().name() + ":" + step.getDirection().name();
            PaymentSagaStepEntity stepEntity = existingSteps.getOrDefault(key, new PaymentSagaStepEntity());
            stepEntity.setPaymentSaga(entity);
            stepEntity.setStepName(step.getStepName().name());
            stepEntity.setServiceName(step.getServiceName());
            stepEntity.setStepSequence(step.getSequence());
            stepEntity.setDirection(step.getDirection().name());
            stepEntity.setStatus(step.getStatus().name());
            stepEntity.setErrorCode(step.getErrorCode());
            stepEntity.setErrorMessage(step.getErrorMessage());
            stepEntity.setStartedAt(step.getStartedAt());
            stepEntity.setFinishedAt(step.getFinishedAt());
            updatedSteps.add(stepEntity);
        }

        entity.getSteps().clear();
        entity.getSteps().addAll(updatedSteps);
    }

    private String stepKey(PaymentSagaStepEntity entity) {
        return entity.getStepName() + ":" + entity.getDirection();
    }

    private PaymentSaga mapToDomain(PaymentSagaEntity entity) {
        return PaymentSaga.builder()
                .paymentId(UUID.fromString(entity.getPaymentId()))
                .sagaId(UUID.fromString(entity.getSagaId()))
                .orderId(entity.getOrderId())
                .customerId(entity.getCustomerId())
                .currency(entity.getCurrency())
                .totalAmount(entity.getTotalAmount())
                .paymentMethod(PaymentMethod.builder()
                        .type(entity.getPaymentMethodType())
                        .token(entity.getPaymentToken())
                        .cardLast4(entity.getCardLast4())
                        .installments(entity.getInstallments())
                        .build())
                .items(entity.getItems().stream()
                        .map(item -> PaymentItem.builder()
                                .sku(item.getSku())
                                .name(item.getName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .toList())
                .shippingAddress(ShippingAddress.builder()
                        .street(entity.getShippingStreet())
                        .city(entity.getShippingCity())
                        .state(entity.getShippingState())
                        .country(entity.getShippingCountry())
                        .postalCode(entity.getShippingPostalCode())
                        .reference(entity.getShippingReference())
                        .build())
                .status(PaymentSagaStatus.valueOf(entity.getStatus()))
                .currentStep(SagaStepName.valueOf(entity.getCurrentStep()))
                .failureReason(entity.getFailureReason())
                .compensationReason(entity.getCompensationReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .steps(entity.getSteps().stream()
                        .sorted(java.util.Comparator.comparing(PaymentSagaStepEntity::getStepSequence))
                        .map(step -> PaymentStep.builder()
                                .stepName(SagaStepName.valueOf(step.getStepName()))
                                .serviceName(step.getServiceName())
                                .sequence(step.getStepSequence())
                                .direction(SagaStepDirection.valueOf(step.getDirection()))
                                .status(SagaStepExecutionStatus.valueOf(step.getStatus()))
                                .startedAt(step.getStartedAt())
                                .finishedAt(step.getFinishedAt())
                                .errorCode(step.getErrorCode())
                                .errorMessage(step.getErrorMessage())
                                .build())
                        .toList())
                .build();
    }
}

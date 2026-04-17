package org.teamokaren.application.usecase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.teamokaren.application.service.PaymentSagaResponseMapper;
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
import org.teamokaren.payments.api.model.OrderItemRequest;
import org.teamokaren.payments.api.model.PaymentSagaCreateRequest;
import org.teamokaren.payments.api.model.PaymentSagaResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatePaymentSagaUseCase {

    private final PaymentSagaRepositoryPort paymentSagaRepository;
    private final PaymentSagaResponseMapper responseMapper;

    public Mono<PaymentSagaResponse> execute(PaymentSagaCreateRequest request) {
        return Mono.fromCallable(() -> createSaga(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(responseMapper::toResponse);
    }

    private PaymentSaga createSaga(PaymentSagaCreateRequest request) {
        Instant now = Instant.now();
        PaymentSaga paymentSaga = PaymentSaga.builder()
                .paymentId(UUID.randomUUID())
                .sagaId(UUID.randomUUID())
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .currency(request.getCurrency())
                .totalAmount(java.math.BigDecimal.valueOf(request.getTotalAmount()))
                .paymentMethod(mapPaymentMethod(request))
                .items(mapItems(request.getItems()))
                .shippingAddress(mapShippingAddress(request))
                .metadata(request.getMetadata())
                .status(PaymentSagaStatus.RECEIVED)
                .currentStep(SagaStepName.CREATE_ORDER)
                .createdAt(now)
                .updatedAt(now)
                .steps(new ArrayList<>())
                .build();

        List<PaymentStep> executedSteps = new ArrayList<>();
        List<PaymentStep> sagaSteps = new ArrayList<>();
        SagaStepName failureStep = resolveFailureStep(request.getMetadata());

        for (PaymentStep templateStep : forwardTemplates()) {
            PaymentStep inProgressStep = templateStep.toBuilder()
                    .status(SagaStepExecutionStatus.IN_PROGRESS)
                    .startedAt(Instant.now())
                    .finishedAt(Instant.now())
                    .build();

            if (failureStep == templateStep.getStepName()) {
                PaymentStep failedStep = inProgressStep.toBuilder()
                    .status(SagaStepExecutionStatus.FAILED)
                        .errorCode("SIMULATED_FAILURE")
                        .errorMessage("Fallo simulado en el paso " + templateStep.getStepName())
                        .build();
                sagaSteps.add(failedStep);
                paymentSaga.setStatus(PaymentSagaStatus.FAILED);
                paymentSaga.setCurrentStep(templateStep.getStepName());
                paymentSaga.setFailureReason(failedStep.getErrorMessage());
                log.warn("Saga {} falló en {}", paymentSaga.getPaymentId(), templateStep.getStepName());

                if (!executedSteps.isEmpty()) {
                    List<PaymentStep> compensationSteps = compensationTemplates(executedSteps);
                    sagaSteps.addAll(compensationSteps);
                    paymentSaga.setStatus(PaymentSagaStatus.COMPENSATED);
                    paymentSaga.setCurrentStep(compensationSteps.get(compensationSteps.size() - 1).getStepName());
                    paymentSaga.setCompensationReason("Compensación automática por fallo en la saga");
                }

                paymentSaga.setSteps(sagaSteps);
                paymentSaga.setUpdatedAt(Instant.now());
                return paymentSagaRepository.save(paymentSaga);
            }

            PaymentStep completedStep = resolveForwardStatus(inProgressStep);
            sagaSteps.add(completedStep);
            executedSteps.add(completedStep);
            paymentSaga.setCurrentStep(templateStep.getStepName());
            paymentSaga.setStatus(resolveSagaStatus(templateStep.getStepName()));
        }

        paymentSaga.setStatus(PaymentSagaStatus.COMPLETED);
        paymentSaga.setCurrentStep(SagaStepName.DELIVER_ORDER);
        paymentSaga.setSteps(sagaSteps);
        paymentSaga.setUpdatedAt(Instant.now());
        log.info("Saga {} completada para orden {}", paymentSaga.getPaymentId(), paymentSaga.getOrderId());
        return paymentSagaRepository.save(paymentSaga);
    }

    private PaymentStep resolveForwardStatus(PaymentStep step) {
        return step.toBuilder()
                .status(SagaStepExecutionStatus.COMPLETED)
                .build();
    }

    private PaymentSagaStatus resolveSagaStatus(SagaStepName stepName) {
        return switch (stepName) {
            case CREATE_ORDER, PROCESS_PAYMENT -> PaymentSagaStatus.PROCESSING_PAYMENT;
            case UPDATE_INVENTORY -> PaymentSagaStatus.RESERVING_INVENTORY;
            case DELIVER_ORDER -> PaymentSagaStatus.SCHEDULING_DELIVERY;
            default -> PaymentSagaStatus.RECEIVED;
        };
    }

    private List<PaymentStep> forwardTemplates() {
        return List.of(
                step(SagaStepName.CREATE_ORDER, "order-service", 1, SagaStepDirection.FORWARD),
                step(SagaStepName.PROCESS_PAYMENT, "payment-service", 2, SagaStepDirection.FORWARD),
                step(SagaStepName.UPDATE_INVENTORY, "inventory-service", 3, SagaStepDirection.FORWARD),
                step(SagaStepName.DELIVER_ORDER, "delivery-service", 4, SagaStepDirection.FORWARD)
        );
    }

    private List<PaymentStep> compensationTemplates(List<PaymentStep> executedSteps) {
        List<PaymentStep> compensations = new ArrayList<>();
        int sequence = 1;
        for (int index = executedSteps.size() - 1; index >= 0; index--) {
            PaymentStep executedStep = executedSteps.get(index);
            compensations.add(step(compensationOf(executedStep.getStepName()), executedStep.getServiceName(), sequence++, SagaStepDirection.COMPENSATION)
                    .toBuilder()
                    .status(SagaStepExecutionStatus.COMPENSATED)
                    .startedAt(Instant.now())
                    .finishedAt(Instant.now())
                    .build());
        }
        return compensations;
    }

    private SagaStepName compensationOf(SagaStepName stepName) {
        return switch (stepName) {
            case CREATE_ORDER -> SagaStepName.CANCEL_ORDER;
            case PROCESS_PAYMENT -> SagaStepName.REVERSE_PAYMENT;
            case UPDATE_INVENTORY -> SagaStepName.REVERSE_INVENTORY;
            case DELIVER_ORDER -> SagaStepName.CANCEL_DELIVERY;
            default -> stepName;
        };
    }

    private SagaStepName resolveFailureStep(Map<String, String> metadata) {
        if (metadata == null || !StringUtils.hasText(metadata.get("simulateFailureStep"))) {
            return null;
        }
        String normalized = metadata.get("simulateFailureStep")
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        return SagaStepName.valueOf(normalized);
    }

    private PaymentMethod mapPaymentMethod(PaymentSagaCreateRequest request) {
        return PaymentMethod.builder()
                .type(request.getPaymentMethod().getType().getValue())
                .token(request.getPaymentMethod().getToken())
                .cardLast4(request.getPaymentMethod().getCardLast4())
                .installments(request.getPaymentMethod().getInstallments())
                .build();
    }

    private List<PaymentItem> mapItems(List<OrderItemRequest> items) {
        return items.stream()
                .map(item -> PaymentItem.builder()
                        .sku(item.getSku())
                        .name(item.getName())
                        .quantity(item.getQuantity())
                    .unitPrice(java.math.BigDecimal.valueOf(item.getUnitPrice()))
                        .build())
                .toList();
    }

    private ShippingAddress mapShippingAddress(PaymentSagaCreateRequest request) {
        return ShippingAddress.builder()
                .street(request.getShippingAddress().getStreet())
                .city(request.getShippingAddress().getCity())
                .state(request.getShippingAddress().getState())
                .country(request.getShippingAddress().getCountry())
                .postalCode(request.getShippingAddress().getPostalCode())
                .reference(request.getShippingAddress().getReference())
                .build();
    }

    private PaymentStep step(SagaStepName stepName, String serviceName, int sequence, SagaStepDirection direction) {
        return PaymentStep.builder()
                .stepName(stepName)
                .serviceName(serviceName)
                .sequence(sequence)
                .direction(direction)
            .status(SagaStepExecutionStatus.PENDING)
                .build();
    }
}

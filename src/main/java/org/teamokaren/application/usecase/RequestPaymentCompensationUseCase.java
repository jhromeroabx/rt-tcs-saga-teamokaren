package org.teamokaren.application.usecase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.teamokaren.application.service.PaymentSagaResponseMapper;
import org.teamokaren.domain.exception.InvalidSagaStateException;
import org.teamokaren.domain.exception.PaymentNotFoundException;
import org.teamokaren.domain.model.PaymentSaga;
import org.teamokaren.domain.model.PaymentSagaStatus;
import org.teamokaren.domain.model.PaymentStep;
import org.teamokaren.domain.model.SagaStepDirection;
import org.teamokaren.domain.model.SagaStepExecutionStatus;
import org.teamokaren.domain.model.SagaStepName;
import org.teamokaren.domain.port.PaymentSagaRepositoryPort;
import org.teamokaren.payments.api.model.CompensationRequest;
import org.teamokaren.payments.api.model.PaymentSagaResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class RequestPaymentCompensationUseCase {

    private final PaymentSagaRepositoryPort paymentSagaRepository;
    private final PaymentSagaResponseMapper responseMapper;

    public Mono<PaymentSagaResponse> execute(UUID paymentId, CompensationRequest request) {
        return Mono.fromCallable(() -> compensate(paymentId, request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(responseMapper::toResponse);
    }

    private PaymentSaga compensate(UUID paymentId, CompensationRequest request) {
        PaymentSaga paymentSaga = paymentSagaRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (EnumSet.of(PaymentSagaStatus.COMPENSATED, PaymentSagaStatus.COMPENSATING).contains(paymentSaga.getStatus())) {
            throw new InvalidSagaStateException("La saga ya fue compensada previamente");
        }

        List<PaymentStep> forwardCompleted = paymentSaga.getSteps().stream()
                .filter(step -> step.getDirection() == SagaStepDirection.FORWARD)
                .filter(step -> step.getStatus() == SagaStepExecutionStatus.COMPLETED)
                .toList();

        if (forwardCompleted.isEmpty()) {
            throw new InvalidSagaStateException("La saga no posee pasos completados para compensar");
        }

        Set<SagaStepName> existingCompensations = paymentSaga.getSteps().stream()
                .filter(step -> step.getDirection() == SagaStepDirection.COMPENSATION)
                .map(PaymentStep::getStepName)
                .collect(java.util.stream.Collectors.toSet());

        List<PaymentStep> appendedSteps = new ArrayList<>(paymentSaga.getSteps());
        int nextSequence = paymentSaga.getSteps().size() + 1;

        for (int index = forwardCompleted.size() - 1; index >= 0; index--) {
            PaymentStep forwardStep = forwardCompleted.get(index);
            SagaStepName compensationStep = compensationOf(forwardStep.getStepName());
            if (existingCompensations.contains(compensationStep)) {
                continue;
            }
            appendedSteps.add(PaymentStep.builder()
                    .stepName(compensationStep)
                    .serviceName(forwardStep.getServiceName())
                    .sequence(nextSequence++)
                    .direction(SagaStepDirection.COMPENSATION)
                    .status(SagaStepExecutionStatus.COMPENSATED)
                    .startedAt(Instant.now())
                    .finishedAt(Instant.now())
                    .build());
        }

        if (appendedSteps.size() == paymentSaga.getSteps().size()) {
            throw new InvalidSagaStateException("No hay pasos nuevos para compensar en el estado actual");
        }

        PaymentSaga compensatedSaga = paymentSaga.toBuilder()
                .status(PaymentSagaStatus.COMPENSATED)
                .currentStep(appendedSteps.get(appendedSteps.size() - 1).getStepName())
                .compensationReason(request.getReason() + " | solicitado por " + request.getRequestedBy())
                .updatedAt(Instant.now())
                .steps(appendedSteps)
                .build();

        return paymentSagaRepository.save(compensatedSaga);
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
}

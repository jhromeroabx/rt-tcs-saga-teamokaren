package org.teamokaren.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.teamokaren.application.service.PaymentSagaResponseMapper;
import org.teamokaren.domain.exception.PaymentNotFoundException;
import org.teamokaren.domain.port.PaymentSagaRepositoryPort;
import org.teamokaren.payments.api.model.PaymentSagaResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPaymentSagaUseCase {

    private final PaymentSagaRepositoryPort paymentSagaRepository;
    private final PaymentSagaResponseMapper responseMapper;

    public Mono<PaymentSagaResponse> execute(UUID paymentId) {
        return Mono.fromCallable(() -> paymentSagaRepository.findById(paymentId)
                        .orElseThrow(() -> new PaymentNotFoundException(paymentId)))
                .subscribeOn(Schedulers.boundedElastic())
                .map(responseMapper::toResponse);
    }
}

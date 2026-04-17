package org.teamokaren.infrastructure.adapter.in.web;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.teamokaren.application.service.IdempotencyService;
import org.teamokaren.application.usecase.CreatePaymentSagaUseCase;
import org.teamokaren.application.usecase.GetPaymentSagaUseCase;
import org.teamokaren.application.usecase.RequestPaymentCompensationUseCase;
import org.teamokaren.payments.api.PaymentsApi;
import org.teamokaren.payments.api.model.CompensationRequest;
import org.teamokaren.payments.api.model.PaymentSagaCreateRequest;
import org.teamokaren.payments.api.model.PaymentSagaResponse;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Validated
public class PaymentsController implements PaymentsApi {

    private final CreatePaymentSagaUseCase createPaymentSagaUseCase;
    private final GetPaymentSagaUseCase getPaymentSagaUseCase;
    private final RequestPaymentCompensationUseCase requestPaymentCompensationUseCase;
    private final IdempotencyService idempotencyService;

    @Override
    public Mono<ResponseEntity<PaymentSagaResponse>> createPaymentSaga(
            String xRequestId,
            Mono<PaymentSagaCreateRequest> paymentSagaCreateRequest,
            ServerWebExchange exchange) {

        return paymentSagaCreateRequest.flatMap(request -> {
            String requestHash = idempotencyService.hash(request);
            Optional<PaymentSagaResponse> replay = idempotencyService.findReplay(xRequestId, requestHash);
            if (replay.isPresent()) {
                return Mono.just(ResponseEntity.ok()
                        .header("X-Idempotent-Replay", "true")
                        .body(replay.get()));
            }

            return createPaymentSagaUseCase.execute(request)
                    .map(response -> {
                        idempotencyService.save(xRequestId, "PAYMENT_CREATE", requestHash, response.getPaymentId(), response, HttpStatus.ACCEPTED.value());
                        return ResponseEntity.accepted()
                                .header(HttpHeaders.LOCATION, "/api/v1/payments/" + response.getPaymentId())
                                .body(response);
                    });
        });
    }

    @Override
    public Mono<ResponseEntity<PaymentSagaResponse>> getPaymentSagaById(UUID paymentId, ServerWebExchange exchange) {
        return getPaymentSagaUseCase.execute(paymentId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<PaymentSagaResponse>> requestPaymentCompensation(
            UUID paymentId,
            String xRequestId,
            Mono<CompensationRequest> compensationRequest,
            ServerWebExchange exchange) {

        return compensationRequest.flatMap(request -> {
            String requestHash = idempotencyService.hash(paymentId.toString() + ":" + request.getReason() + ":" + request.getRequestedBy());
            Optional<PaymentSagaResponse> replay = idempotencyService.findReplay(xRequestId, requestHash);
            if (replay.isPresent()) {
                return Mono.just(ResponseEntity.ok()
                        .header("X-Idempotent-Replay", "true")
                        .body(replay.get()));
            }

            return requestPaymentCompensationUseCase.execute(paymentId, request)
                    .map(response -> {
                        idempotencyService.save(xRequestId, "PAYMENT_COMPENSATION:" + paymentId, requestHash, response.getPaymentId(), response, HttpStatus.ACCEPTED.value());
                        return ResponseEntity.accepted().body(response);
                    });
        });
    }
}

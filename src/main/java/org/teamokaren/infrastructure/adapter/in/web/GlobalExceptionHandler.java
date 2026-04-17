package org.teamokaren.infrastructure.adapter.in.web;

import java.time.OffsetDateTime;
import org.teamokaren.domain.exception.IdempotencyConflictException;
import org.teamokaren.domain.exception.InvalidSagaStateException;
import org.teamokaren.domain.exception.PaymentNotFoundException;
import org.teamokaren.payments.api.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePaymentNotFound(PaymentNotFoundException exception) {
        return Mono.just(
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(error("PAYMENT_NOT_FOUND", exception.getMessage(), null))
        );
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIdempotencyConflict(IdempotencyConflictException exception) {
        return Mono.just(
                ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(error("IDEMPOTENCY_CONFLICT", exception.getMessage(), null))
        );
    }

    @ExceptionHandler(InvalidSagaStateException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInvalidSagaState(InvalidSagaStateException exception) {
        return Mono.just(
                ResponseEntity.unprocessableEntity()
                        .body(error("INVALID_SAGA_STATE", exception.getMessage(), null))
        );
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(WebExchangeBindException ex) {

        String message = ex.getFieldErrors()
                .stream()
                .findFirst()
                .map(this::mapMessage)
                .orElse("Solicitud inválida");

        return Mono.just(
                ResponseEntity
                        .badRequest()
                        .body(error("VALIDATION_ERROR", message, null))
        );
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneric(Exception ex) {
        Throwable cause = rootCause(ex);
        if (cause instanceof PaymentNotFoundException paymentNotFoundException) {
            return handlePaymentNotFound(paymentNotFoundException);
        }
        if (cause instanceof IdempotencyConflictException idempotencyConflictException) {
            return handleIdempotencyConflict(idempotencyConflictException);
        }
        if (cause instanceof InvalidSagaStateException invalidSagaStateException) {
            return handleInvalidSagaState(invalidSagaStateException);
        }

        return Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(error("INTERNAL_ERROR", "Error interno del servidor", cause.getMessage()))
        );
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String mapMessage(FieldError error) {
        return switch (error.getField()) {
            case "orderId" -> "El identificador de la orden es obligatorio";
            case "customerId" -> "El identificador del cliente es obligatorio";
            case "currency" -> "La moneda debe tener 3 caracteres";
            case "totalAmount" -> "El monto total debe ser mayor a cero";
            default -> "Solicitud inválida";
        };
    }

    private ErrorResponse error(String code, String message, String detail) {
        ErrorResponse errorResponse = new ErrorResponse(code, message, OffsetDateTime.now());
        if (detail != null) {
            errorResponse.setDetail(detail);
        }
        return errorResponse;
    }
}
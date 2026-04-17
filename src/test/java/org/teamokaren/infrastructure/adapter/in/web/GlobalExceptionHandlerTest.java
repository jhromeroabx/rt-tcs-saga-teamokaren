package org.teamokaren.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.teamokaren.domain.exception.InvalidSagaStateException;
import org.teamokaren.payments.api.model.ErrorResponse;
import reactor.core.publisher.Mono;

class GlobalExceptionHandlerTest {

    @Test
    void shouldReturnUnprocessableEntityForWrappedInvalidSagaStateException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Mono<ResponseEntity<ErrorResponse>> response = handler.handleGeneric(
                new RuntimeException("wrapper", new InvalidSagaStateException("La saga ya fue compensada previamente")));

        ResponseEntity<ErrorResponse> entity = response.block();

        assertThat(entity).isNotNull();
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getCode()).isEqualTo("INVALID_SAGA_STATE");
    }
}
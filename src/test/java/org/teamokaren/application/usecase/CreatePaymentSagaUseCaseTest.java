package org.teamokaren.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.teamokaren.application.service.PaymentSagaResponseMapper;
import org.teamokaren.domain.port.PaymentSagaRepositoryPort;
import org.teamokaren.payments.api.model.OrderItemRequest;
import org.teamokaren.payments.api.model.PaymentMethodRequest;
import org.teamokaren.payments.api.model.PaymentSagaCreateRequest;
import org.teamokaren.payments.api.model.PaymentSagaStatus;
import org.teamokaren.payments.api.model.SagaStepStatus;
import org.teamokaren.payments.api.model.ShippingAddressRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CreatePaymentSagaUseCaseTest {

    @Test
    void shouldCreateCompletedPaymentSaga() {
        PaymentSagaRepositoryPort repository = mock(PaymentSagaRepositoryPort.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePaymentSagaUseCase useCase = new CreatePaymentSagaUseCase(repository, new PaymentSagaResponseMapper());

        Mono<?> result = useCase.execute(baseRequest(null));

        StepVerifier.create(result)
                .assertNext(response -> {
                    org.teamokaren.payments.api.model.PaymentSagaResponse sagaResponse =
                            (org.teamokaren.payments.api.model.PaymentSagaResponse) response;
                    assertEquals(PaymentSagaStatus.COMPLETED, sagaResponse.getStatus());
                    assertEquals(4, sagaResponse.getSteps().size());
                    assertEquals("ORD-100045", sagaResponse.getOrderId());
                })
                .verifyComplete();
    }

    @Test
    void shouldCompensateWhenFailureIsSimulated() {
        PaymentSagaRepositoryPort repository = mock(PaymentSagaRepositoryPort.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePaymentSagaUseCase useCase = new CreatePaymentSagaUseCase(repository, new PaymentSagaResponseMapper());

        Mono<?> result = useCase.execute(baseRequest(Map.of("simulateFailureStep", "UPDATE_INVENTORY")));

        StepVerifier.create(result)
                .assertNext(response -> {
                    org.teamokaren.payments.api.model.PaymentSagaResponse sagaResponse =
                            (org.teamokaren.payments.api.model.PaymentSagaResponse) response;
                    assertEquals(PaymentSagaStatus.COMPENSATED, sagaResponse.getStatus());
                    assertTrue(sagaResponse.getSteps().stream().anyMatch(step -> step.getStatus().name().equals("FAILED")));
                    assertTrue(sagaResponse.getSteps().stream().anyMatch(step -> step.getDirection().name().equals("COMPENSATION")));
                })
                .verifyComplete();
    }

    private PaymentSagaCreateRequest baseRequest(Map<String, String> metadata) {
        PaymentMethodRequest paymentMethod = new PaymentMethodRequest();
        paymentMethod.setType(PaymentMethodRequest.TypeEnum.CARD);
        paymentMethod.setToken("tok_01HZX9J4P3A1");
        paymentMethod.setCardLast4("4242");
        paymentMethod.setInstallments(1);

        OrderItemRequest item = new OrderItemRequest();
        item.setSku("SKU-1000");
        item.setName("Audífonos Bluetooth");
        item.setQuantity(2);
        item.setUnitPrice(74.95d);

        ShippingAddressRequest shippingAddress = new ShippingAddressRequest();
        shippingAddress.setStreet("Av. Primavera 123");
        shippingAddress.setCity("Lima");
        shippingAddress.setState("Lima");
        shippingAddress.setCountry("PE");
        shippingAddress.setPostalCode("15046");
        shippingAddress.setReference("Torre 2, recepción");

        PaymentSagaCreateRequest request = new PaymentSagaCreateRequest();
        request.setOrderId("ORD-100045");
        request.setCustomerId("CUS-9001");
        request.setCurrency("PEN");
        request.setTotalAmount(new BigDecimal("149.90").doubleValue());
        request.setPaymentMethod(paymentMethod);
        request.setItems(List.of(item));
        request.setShippingAddress(shippingAddress);
        request.setMetadata(metadata);
        return request;
    }
}

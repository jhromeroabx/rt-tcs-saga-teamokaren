package org.teamokaren.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSaga {
    private UUID paymentId;
    private UUID sagaId;
    private String orderId;
    private String customerId;
    private String currency;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private List<PaymentItem> items;
    private ShippingAddress shippingAddress;
    private Map<String, String> metadata;
    private PaymentSagaStatus status;
    private SagaStepName currentStep;
    private String failureReason;
    private String compensationReason;
    private Instant createdAt;
    private Instant updatedAt;
    private List<PaymentStep> steps;
}

package org.teamokaren.infrastructure.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payment_saga")
@Getter
@Setter
public class PaymentSagaEntity {
    @Id
    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "saga_id", nullable = false, unique = true)
    private String sagaId;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_method_type", nullable = false)
    private String paymentMethodType;

    @Column(name = "payment_token", nullable = false)
    private String paymentToken;

    @Column(name = "card_last4")
    private String cardLast4;

    private Integer installments;

    @Column(nullable = false)
    private String status;

    @Column(name = "current_step", nullable = false)
    private String currentStep;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "compensation_reason")
    private String compensationReason;

    @Column(name = "shipping_street", nullable = false)
    private String shippingStreet;

    @Column(name = "shipping_city", nullable = false)
    private String shippingCity;

    @Column(name = "shipping_state")
    private String shippingState;

    @Column(name = "shipping_country", nullable = false)
    private String shippingCountry;

    @Column(name = "shipping_postal_code", nullable = false)
    private String shippingPostalCode;

    @Column(name = "shipping_reference")
    private String shippingReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "paymentSaga", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PaymentItemEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "paymentSaga", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PaymentSagaStepEntity> steps = new ArrayList<>();
}

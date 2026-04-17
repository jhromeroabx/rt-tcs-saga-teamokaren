package org.teamokaren.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payment_saga_step")
@Getter
@Setter
public class PaymentSagaStepEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "payment_id")
    private PaymentSagaEntity paymentSaga;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "step_sequence", nullable = false)
    private Integer stepSequence;

    @Column(nullable = false)
    private String direction;

    @Column(nullable = false)
    private String status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}

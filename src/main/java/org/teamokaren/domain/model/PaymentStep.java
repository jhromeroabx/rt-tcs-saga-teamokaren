package org.teamokaren.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStep {
    private SagaStepName stepName;
    private String serviceName;
    private Integer sequence;
    private SagaStepDirection direction;
    private SagaStepExecutionStatus status;
    private Instant startedAt;
    private Instant finishedAt;
    private String errorCode;
    private String errorMessage;
}

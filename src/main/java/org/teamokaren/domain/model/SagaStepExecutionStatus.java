package org.teamokaren.domain.model;

public enum SagaStepExecutionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    COMPENSATED,
    SKIPPED
}

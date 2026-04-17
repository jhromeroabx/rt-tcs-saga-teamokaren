package org.teamokaren.domain.model;

public enum PaymentSagaStatus {
    RECEIVED,
    PROCESSING_PAYMENT,
    RESERVING_INVENTORY,
    SCHEDULING_DELIVERY,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}

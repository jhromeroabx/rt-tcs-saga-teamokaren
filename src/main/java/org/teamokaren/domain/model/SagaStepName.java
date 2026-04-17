package org.teamokaren.domain.model;

public enum SagaStepName {
    CREATE_ORDER,
    PROCESS_PAYMENT,
    UPDATE_INVENTORY,
    DELIVER_ORDER,
    CANCEL_ORDER,
    REVERSE_PAYMENT,
    REVERSE_INVENTORY,
    CANCEL_DELIVERY
}

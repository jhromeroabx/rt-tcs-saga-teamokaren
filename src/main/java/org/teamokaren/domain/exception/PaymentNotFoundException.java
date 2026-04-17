package org.teamokaren.domain.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(UUID paymentId) {
        super("No existe el pago con id " + paymentId);
    }
}

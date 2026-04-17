package org.teamokaren.domain.port;

import java.util.Optional;
import java.util.UUID;
import org.teamokaren.domain.model.PaymentSaga;

public interface PaymentSagaRepositoryPort {
    PaymentSaga save(PaymentSaga paymentSaga);
    Optional<PaymentSaga> findById(UUID paymentId);
}

package org.teamokaren.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentSagaRepository extends JpaRepository<PaymentSagaEntity, String> {
}

package org.teamokaren.domain.port;

import java.util.Optional;
import org.teamokaren.domain.model.IdempotencyRecord;

public interface IdempotencyPort {

    Optional<IdempotencyRecord> find(String requestId);

    void save(IdempotencyRecord record);
}
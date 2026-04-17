package org.teamokaren.domain.exception;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super("La clave idempotente ya fue utilizada con un payload distinto");
    }
}

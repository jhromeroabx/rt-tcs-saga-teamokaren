package org.teamokaren.domain.exception;

public class InvalidSagaStateException extends RuntimeException {
    public InvalidSagaStateException(String message) {
        super(message);
    }
}

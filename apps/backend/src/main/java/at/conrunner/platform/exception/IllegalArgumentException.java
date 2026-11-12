package at.conrunner.platform.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IllegalArgumentException extends RuntimeException {
    public IllegalArgumentException(String message) {
        super(message);
    }

    public IllegalArgumentException(String logMessage, String message) {
        super(message);
        log.error(logMessage);
    }
}

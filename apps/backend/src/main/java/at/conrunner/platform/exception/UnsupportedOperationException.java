package at.conrunner.platform.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UnsupportedOperationException extends RuntimeException {
    public UnsupportedOperationException(String message) {
        super(message);
    }

    public UnsupportedOperationException(String logMessage, String message) {
        super(message);
        log.error(logMessage);
    }
}

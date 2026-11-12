package at.conrunner.platform.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IllegalStateException extends RuntimeException {
    public IllegalStateException(String logMessage) {
        super();
        log.error(logMessage);
    }
}

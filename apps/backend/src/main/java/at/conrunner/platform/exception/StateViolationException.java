package at.conrunner.platform.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StateViolationException extends RuntimeException {
    public StateViolationException(String logMessage) {
        super(logMessage);
    }
}

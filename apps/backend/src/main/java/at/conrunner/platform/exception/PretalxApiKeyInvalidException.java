package at.conrunner.platform.exception;

public class PretalxApiKeyInvalidException extends RuntimeException {
    public PretalxApiKeyInvalidException(String message) {
        super(message);
    }

    public PretalxApiKeyInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}

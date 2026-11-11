package at.control.shiftcontrol.platform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Object dto;

    public ConflictException() {
        super();
        this.dto = null;
    }

    public ConflictException(String message) {
        super(message);
        this.dto = null;
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
        this.dto = null;
    }

    public ConflictException(Exception e) {
        super(e);
        this.dto = null;
    }

    /**
     * Create an exception that carries a DTO object (no message).
     * The DTO can be retrieved by calling {@link #getDto()}.
     */
    public ConflictException(Object dto) {
        super();
        this.dto = dto;
    }

    /**
     * Create an exception with both a message and a DTO.
     */
    public ConflictException(String message, Object dto) {
        super(message);
        this.dto = dto;
    }

    public Object getDto() {
        return dto;
    }

    public boolean hasDto() {
        return this.dto != null;
    }
}

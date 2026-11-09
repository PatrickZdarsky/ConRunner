package at.control.shiftcontrol.shift.config;

import java.lang.invoke.MethodHandles;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.control.shiftcontrol.shift.lib.exception.BadRequestException;
import at.control.shiftcontrol.shift.lib.exception.ConflictException;
import at.control.shiftcontrol.shift.lib.exception.FileExportException;
import at.control.shiftcontrol.shift.lib.exception.ForbiddenException;
import at.control.shiftcontrol.shift.lib.exception.NotFoundException;
import at.control.shiftcontrol.shift.lib.exception.NotificationSettingAlreadyExistsException;
import at.control.shiftcontrol.shift.lib.exception.PartiallyNotFoundException;
import at.control.shiftcontrol.shift.lib.exception.PretalxApiKeyInvalidException;
import at.control.shiftcontrol.shift.lib.exception.StateViolationException;
import at.control.shiftcontrol.shift.lib.exception.UnauthorizedException;
import at.control.shiftcontrol.shift.lib.exception.ValidationException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleAllUnhandledExceptions(
        Exception ex,
        WebRequest request
    ) {
        LOGGER.error("Unhandled exception", ex);
        return handleExceptionInternal(
            ex,
            new ApiErrorDto("An unexpected error occurred"),
            new HttpHeaders(),
            INTERNAL_SERVER_ERROR,
            request
        );
    }

    /**
     * Use the @ExceptionHandler annotation to write handler for custom exceptions.
     */
    @ExceptionHandler({NotFoundException.class})
    protected ResponseEntity<Object> handleNotFound(Exception ex, WebRequest request) {
        return handleInternal(ex, request, NOT_FOUND);
    }

    @ExceptionHandler(value = {ValidationException.class})
    protected ResponseEntity<Object> handleValidation(Exception ex, WebRequest request) {
        return handleInternal(ex, request, UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(value = {ConflictException.class})
    protected ResponseEntity<Object> handleConflict(Exception ex, WebRequest request) {
        LOGGER.warn(ex.getMessage());
        var conflictException = (ConflictException) ex;
        Object body = conflictException.hasDto() ? conflictException.getDto() : new ApiErrorDto(conflictException.getMessage());
        return handleExceptionInternal(conflictException, body, new HttpHeaders(), CONFLICT, request);
    }

    @ExceptionHandler(value = {PartiallyNotFoundException.class, NotificationSettingAlreadyExistsException.class})
    public ResponseEntity<Object> handleConflicts(Exception ex, WebRequest request) {
        return handleInternal(ex, request, CONFLICT);
    }

    @ExceptionHandler(value = {ForbiddenException.class, AuthorizationDeniedException.class})
    protected ResponseEntity<Object> handleForbidden(Exception ex, WebRequest request) {
        return handleInternal(ex, request, FORBIDDEN);
    }

    @ExceptionHandler(value = {UnauthorizedException.class})
    protected ResponseEntity<Object> handleUnauthorized(Exception ex, WebRequest request) {
        return handleInternal(ex, request, UNAUTHORIZED);
    }

    @ExceptionHandler(value = {BadRequestException.class, StateViolationException.class})
    protected ResponseEntity<Object> handleBadRequest(Exception ex, WebRequest request) {
        return handleInternal(ex, request, BAD_REQUEST);
    }

    @ExceptionHandler(value = {FileExportException.class})
    protected ResponseEntity<Object> handleFileExport(Exception ex, WebRequest request) {
        return handleInternal(ex, request, INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {UnsupportedOperationException.class})
    protected ResponseEntity<Object> handleUnsupportedOperationException(Exception ex, WebRequest request) {
        return handleInternal(ex, request, BAD_REQUEST);
    }

    @ExceptionHandler(value = {PretalxApiKeyInvalidException.class})
    protected ResponseEntity<Object> handlePretalxApiKeyInvalidException(Exception ex, WebRequest request) {
        return handleInternal(ex, request, BAD_REQUEST);
    }

    private ResponseEntity<Object> handleInternal(Exception ex, WebRequest request, HttpStatus status) {
        LOGGER.warn(ex.getMessage());
        return handleExceptionInternal(ex, new ApiErrorDto(ex.getMessage()), new HttpHeaders(), status, request);
    }

    /**
     * Override methods from ResponseEntityExceptionHandler to send a customized HTTP response for a know exception
     * from e.g. Spring
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        // If you want to keep your ApiErrorDto shape minimal:
        // pick the first message, or join them.
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + " " + err.getDefaultMessage()) // <-- add space
            .collect(Collectors.joining(", "));

        return new ResponseEntity<>(new ApiErrorDto(msg), headers, status);
    }
}

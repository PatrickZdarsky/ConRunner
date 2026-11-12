package at.conrunner.platform.exception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.Getter;

/**
 * Exception representing one or more validation errors.
 *
 * <p>
 * Usage:
 * <code>
 * ValidationException.Builder b = ValidationException.builder()
 * .context("Create User")
 * .error("email", "must be a valid email")
 * .error("password", "must be at least 8 characters");
 * b.throwIfInvalid();
 * </code>
 */
@Getter
@ResponseStatus(HttpStatus.CONFLICT)
public class ValidationException extends RuntimeException {
    private final String context;
    private final transient Map<String, Collection<?>> validationErrors;

    public ValidationException() {
        super();
        this.context = null;
        this.validationErrors = Collections.emptyMap();
    }

    public ValidationException(String message) {
        super(message);
        this.context = null;
        this.validationErrors = Collections.emptyMap();
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.context = null;
        this.validationErrors = Collections.emptyMap();
    }

    public ValidationException(Exception e) {
        super(e);
        this.context = null;
        this.validationErrors = Collections.emptyMap();
    }

    private ValidationException(String context, Map<String, ? extends Collection<?>> validationErrors) {
        super(buildMessage(context, validationErrors));
        this.context = context;
        this.validationErrors = Collections.unmodifiableMap(validationErrors);
    }

    private static String buildMessage(String context, Map<String, ? extends Collection<?>> validationErrors) {
        if (validationErrors == null || validationErrors.isEmpty()) {
            return (context == null || context.isBlank())
                ? "Validation failed."
                : context + " failed due to validation errors.";
        }
        StringBuilder sb = new StringBuilder();
        if (context != null && !context.isBlank()) {
            sb.append(context).append(" failed. ");
        }
        sb.append("Validation errors:\n");
        validationErrors.forEach((field, messages) -> {
            String joined = messages.stream()
                .map(Objects::toString)
                .collect(Collectors.joining("; "));
            sb.append(" - ").append(field).append(": ").append(joined).append("\n");
        });
        return sb.toString().trim();
    }

    public Map<String, Collection<?>> getValidationErrors() {
        return validationErrors == null ? Map.of() : validationErrors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static <T> ValidationException of(String field, Collection<T> messages) {
        return ValidationException.builder()
            .error(field, messages)
            .build();
    }

    @SafeVarargs
    public static <T> ValidationException of(String field, T... messages) {
        return ValidationException.builder()
            .error(field, messages)
            .build();
    }

    /**
     * Merge multiple {@link ValidationException}s into one.
     */
    public static ValidationException combine(ValidationException... exceptions) {
        Builder builder = builder();
        for (ValidationException e : exceptions) {
            if (e == null) {
                continue;
            }
            if (e.context != null && !e.context.isBlank() && (builder.context == null || builder.context.isBlank())) {
                builder.context(e.context);
            }
            e.getValidationErrors().forEach(builder::error);
        }
        return builder.build();
    }

    public static class Builder {
        private final Map<String, Collection<Object>> errors = new LinkedHashMap<>();
        private String context;

        public Builder context(String context) {
            this.context = context;
            return this;
        }

        public <T> Builder error(String field, Collection<T> messages) {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException("field must not be null or blank");
            }
            if (messages != null && !messages.isEmpty()) {
                errors.computeIfAbsent(field, k -> new ArrayList<>()).addAll(messages);
            }
            return this;
        }

        @SafeVarargs
        public final <T> Builder error(String field, T... messages) {
            if (messages != null && messages.length > 0) {
                error(field, Arrays.asList(messages));
            }
            return this;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public ValidationException build() {
            return new ValidationException(context, errors);
        }

        public void throwIfInvalid() throws ValidationException {
            if (hasErrors()) {
                throw build();
            }
        }
    }
}

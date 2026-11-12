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

import org.slf4j.LoggerFactory;

/**
 * Exception indicating that some, but not all, required entities could not be resolved.
 *
 * <p>
 * Usage pattern:
 *
 * <pre>{@code
 * PartiallyNotFoundException.Builder missing = PartiallyNotFoundException.builder()
 *     .context("User assignment")
 *     .missing("User", missingUserIds)
 *     .missing("Group", "admins", "mods");
 *
 * missing.throwIfMissing();
 * }</pre>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PartiallyNotFoundException extends RuntimeException {
    private final String context;
    private final transient Map<String, Collection<?>> missingEntities;

    private PartiallyNotFoundException(String context, Map<String, ? extends Collection<?>> missingEntities) {
        super(buildMessage(context, missingEntities));
        this.context = context;
        this.missingEntities = Collections.unmodifiableMap(missingEntities);
    }

    private static String buildMessage(String context, Map<String, ? extends Collection<?>> missingEntities) {
        if (missingEntities != null && !missingEntities.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (context != null && !context.isBlank()) {
                sb.append(context).append(" failed. ");
            }
            sb.append("Missing entities:\n");
            missingEntities.forEach((type, values) -> {
                String joined = values.stream()
                    .map(Objects::toString)
                    .collect(Collectors.joining(", "));
                sb.append(" - ").append(type).append(": ").append(joined).append("\n");
            });
            LoggerFactory.getLogger(sb.toString().trim());
        }
        return "Some required entities were not found.";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static <T> PartiallyNotFoundException of(String entityType, Collection<T> unresolved) {
        return PartiallyNotFoundException.builder()
            .missing(entityType, unresolved)
            .build();
    }
    // === BUILDER ===

    @SafeVarargs
    public static <T> PartiallyNotFoundException of(String entityType, T... unresolved) {
        return PartiallyNotFoundException.builder()
            .missing(entityType, unresolved)
            .build();
    }

    /**
     * Merge multiple {@link PartiallyNotFoundException}s into one,
     * useful when resolving in separate layers or steps.
     */
    public static PartiallyNotFoundException combine(PartiallyNotFoundException... exceptions) {
        Builder builder = builder();
        for (PartiallyNotFoundException e : exceptions) {
            if (e == null) {
                continue;
            }
            if (e.context != null && !e.context.isBlank() && (builder.context == null || builder.context.isBlank())) {
                builder.context(e.context);
            }
            e.getMissingEntities().forEach(builder::missing);
        }
        return builder.build();
    }
    // === CONVENIENCE FACTORIES ===

    public String getContext() {
        return context;
    }

    public Map<String, Collection<?>> getMissingEntities() {
        return missingEntities == null ? Map.of() : missingEntities;
    }

    public static class Builder {
        private final Map<String, Collection<Object>> missing = new LinkedHashMap<>();
        private String context;

        /**
         * High-level description of what you were doing when resolution failed.
         * Example: "User assignment", "Order checkout", "Batch import"
         */
        public Builder context(String context) {
            this.context = context;
            return this;
        }

        /**
         * Register missing values for a given logical entity type.
         *
         * @param entityType logical label, e.g. "User", "Role", "Group"
         * @param unresolved missing identifiers or objects
         */
        public <T> Builder missing(String entityType, Collection<T> unresolved) {
            if (entityType == null || entityType.isBlank()) {
                throw new IllegalArgumentException("entityType must not be null or blank");
            }
            if (unresolved != null && !unresolved.isEmpty()) {
                missing.computeIfAbsent(entityType, k -> new ArrayList<>()).addAll(unresolved);
            }
            return this;
        }

        /**
         * Varargs convenience for {@link #missing(String, Collection)}.
         */
        @SafeVarargs
        public final <T> Builder missing(String entityType, T... unresolved) {
            if (unresolved != null && unresolved.length > 0) {
                missing(entityType, Arrays.asList(unresolved));
            }
            return this;
        }

        public boolean hasMissing() {
            return !missing.isEmpty();
        }

        public PartiallyNotFoundException build() {
            return new PartiallyNotFoundException(context, missing);
        }

        /**
         * Throws a {@link PartiallyNotFoundException} if any missing entries were registered.
         */
        public void throwIfMissing() throws PartiallyNotFoundException {
            if (hasMissing()) {
                throw build();
            }
        }
    }
}

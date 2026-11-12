package at.conrunner.shift.lib.event;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
public abstract class BaseEvent {
    private final EventType eventType;
    @JsonIgnore
    private final String routingKey;

    @Nullable
    private String description;
    private String actingUserId;
    private String traceId;
    private Instant timestamp = Instant.now();

    public BaseEvent withActingUserId(String actingUserId) {
        this.actingUserId = actingUserId;
        return this;
    }

    public <T extends BaseEvent> T withDescription(String description) {
        this.description = description;
        return (T) this;
    }
}

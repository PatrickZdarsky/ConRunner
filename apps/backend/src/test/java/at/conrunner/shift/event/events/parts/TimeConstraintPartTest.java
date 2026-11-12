package at.conrunner.shift.event.events.parts;

import at.conrunner.shift.entity.TimeConstraint;
import at.conrunner.shift.lib.event.events.parts.TimeConstraintPart;
import at.conrunner.shift.lib.type.TimeConstraintType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeConstraintPartTest {

    @Test
    void of() {
        // Arrange
        TimeConstraint timeConstraint = new TimeConstraint();
        timeConstraint.setId(1L);
        timeConstraint.setType(TimeConstraintType.UNAVAILABLE);
        timeConstraint.setStartTime(Instant.parse("2023-01-01T10:00:00Z"));
        timeConstraint.setEndTime(Instant.parse("2023-01-01T12:00:00Z"));

        // Act
        TimeConstraintPart timeConstraintPart = TimeConstraintPart.of(timeConstraint);

        // Assert
        assertEquals(String.valueOf(timeConstraint.getId()), timeConstraintPart.getId());
        assertEquals(timeConstraint.getType(), timeConstraintPart.getType());
        assertEquals(timeConstraint.getStartTime(), timeConstraintPart.getFrom());
        assertEquals(timeConstraint.getEndTime(), timeConstraintPart.getTo());
    }

    @Test
    void of_throwsNullPointerException_forNullTimeConstraint() {
        assertThrows(NullPointerException.class, () -> {
            TimeConstraintPart.of(null);
        });
    }
}


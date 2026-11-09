package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;

import lombok.NoArgsConstructor;

import at.shiftcontrol.lib.entity.Event;
import at.shiftcontrol.lib.entity.TimeConstraint;
import at.shiftcontrol.lib.entity.Volunteer;
import at.shiftcontrol.shiftservice.dto.TimeConstraintCreateDto;
import at.shiftcontrol.shiftservice.dto.TimeConstraintDto;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class TimeConstraintMapper {
    public static TimeConstraintDto toDto(TimeConstraint e) {
        return new TimeConstraintDto(
                String.valueOf(e.getId()),
                e.getType(),
                e.getStartTime(),
                e.getEndTime()
            );
    }

    public static Collection<TimeConstraintDto> toDto(Collection<TimeConstraint> entities) {
        return entities.stream()
            .map(TimeConstraintMapper::toDto)
            .toList();
    }

    // Create entity from create DTO and resolved Attendance
    public static TimeConstraint fromCreateDto(TimeConstraintCreateDto dto, Volunteer volunteer, Event event) {
        return TimeConstraint.builder()
            .event(event)
            .volunteer(volunteer)
            .type(dto.getType())
            .startTime(dto.getFrom())
            .endTime(dto.getTo())
            .build();
    }
}

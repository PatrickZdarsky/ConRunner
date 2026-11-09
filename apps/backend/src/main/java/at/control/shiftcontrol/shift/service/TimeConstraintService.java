package at.control.shiftcontrol.shift.service;

import java.util.Collection;

import lombok.NonNull;

import at.shiftcontrol.shiftservice.dto.TimeConstraintCreateDto;
import at.shiftcontrol.shiftservice.dto.TimeConstraintDto;

public interface TimeConstraintService {
    @NonNull Collection<TimeConstraintDto> getTimeConstraints(@NonNull String userId, long eventId);

    @NonNull TimeConstraintDto createTimeConstraint(@NonNull TimeConstraintCreateDto createDto, @NonNull String userId, long eventId);

    void delete(long timeConstraintId);
}

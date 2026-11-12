package at.conrunner.shift.service;

import java.util.UUID;
import java.util.Collection;

import lombok.NonNull;

import at.conrunner.shift.dto.TimeConstraintCreateDto;
import at.conrunner.shift.dto.TimeConstraintDto;

public interface TimeConstraintService {
    @NonNull Collection<TimeConstraintDto> getTimeConstraints(@NonNull UUID userId, long eventId);

    @NonNull TimeConstraintDto createTimeConstraint(@NonNull TimeConstraintCreateDto createDto, @NonNull UUID userId, long eventId);

    void delete(long timeConstraintId);
}

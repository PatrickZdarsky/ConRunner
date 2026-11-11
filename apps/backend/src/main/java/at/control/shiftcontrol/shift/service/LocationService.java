package at.control.shiftcontrol.shift.service;

import java.util.Collection;

import at.control.shiftcontrol.shift.dto.location.LocationDto;
import at.control.shiftcontrol.shift.dto.location.LocationModificationDto;

import lombok.NonNull;

public interface LocationService {
    @NonNull Collection<LocationDto> getAllLocationsForEvent(long eventId);

    @NonNull LocationDto createLocation(long eventId, @NonNull LocationModificationDto modificationDto);

    @NonNull LocationDto getLocation(long locationId);

    @NonNull LocationDto updateLocation(long locationId, @NonNull LocationModificationDto modificationDto);

    void deleteLocation(long locationId);
}

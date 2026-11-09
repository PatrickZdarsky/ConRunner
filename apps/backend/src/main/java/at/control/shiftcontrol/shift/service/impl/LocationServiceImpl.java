package at.control.shiftcontrol.shift.service.impl;

import java.util.Collection;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.entity.Location;
import at.shiftcontrol.lib.event.events.LocationEvent;
import at.shiftcontrol.lib.exception.BadRequestException;
import at.shiftcontrol.shiftservice.annotation.AdminOnly;
import at.shiftcontrol.shiftservice.dao.EventDao;
import at.shiftcontrol.shiftservice.dao.LocationDao;
import at.shiftcontrol.shiftservice.dto.location.LocationDto;
import at.shiftcontrol.shiftservice.dto.location.LocationModificationDto;
import at.shiftcontrol.shiftservice.mapper.LocationMapper;
import at.shiftcontrol.shiftservice.service.LocationService;
import at.shiftcontrol.shiftservice.util.SecurityHelper;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {
    private final LocationDao locationDao;
    private final EventDao eventDao;
    private final SecurityHelper securityHelper;
    private final ApplicationEventPublisher publisher;

    @Override
    public @NonNull LocationDto getLocation(long locationId) {
        var location = locationDao.getById(locationId);

        securityHelper.assertUserIsAllowedToAccessEvent(location.getEvent());
        return LocationMapper.toLocationDto(location);
    }

    @Override
    public @NonNull Collection<LocationDto> getAllLocationsForEvent(long eventId) {
        securityHelper.assertUserIsAllowedToAccessEvent(eventDao.getById(eventId));
        var locations = locationDao.findAllByEventId(eventId);

        return LocationMapper.toLocationDto(locations.stream().toList());
    }

    @Override
    @AdminOnly
    public @NonNull LocationDto createLocation(long eventId, @NonNull LocationModificationDto modificationDto) {
        var event = eventDao.getById(eventId);

        validateNameUniquenessInEvent(eventId, modificationDto.getName(), null);

        var newLocation = Location.builder()
            .event(event)
            .name(modificationDto.getName())
            .description(modificationDto.getDescription())
            .url(modificationDto.getUrl())
            .readOnly(false)
            .build();

        newLocation = locationDao.save(newLocation);

        publisher.publishEvent(LocationEvent.locationCreated(newLocation));
        return LocationMapper.toLocationDto(newLocation);
    }


    @Override
    @AdminOnly
    public @NonNull LocationDto updateLocation(long locationId, @NonNull LocationModificationDto modificationDto) {
        var location = locationDao.getById(locationId);

        if (location.isReadOnly()) {
            throw new BadRequestException("Cannot modify read-only location");
        }

        validateNameUniquenessInEvent(location.getEvent().getId(), modificationDto.getName(), locationId);

        location.setName(modificationDto.getName());
        location.setDescription(modificationDto.getDescription());
        location.setUrl(modificationDto.getUrl());

        location = locationDao.save(location);

        publisher.publishEvent(LocationEvent.locationUpdated(location));
        return LocationMapper.toLocationDto(location);
    }

    void validateNameUniquenessInEvent(long eventId, String name, Long excludeLocationId) {
        var locationOpt = locationDao.findByEventAndName(eventId, name);
        if (locationOpt.isPresent() && (excludeLocationId == null || locationOpt.get().getId() != excludeLocationId)) {
            throw new BadRequestException("Location name must be unique within an event");
        }
    }

    @Override
    @AdminOnly
    public void deleteLocation(long locationId) {
        var location = locationDao.getById(locationId);

        if (location.isReadOnly()) {
            throw new BadRequestException("Cannot modify read-only location");
        }

        var locationEvent = LocationEvent.locationDeleted(location);
        locationDao.delete(location);
        publisher.publishEvent(locationEvent);
    }
}

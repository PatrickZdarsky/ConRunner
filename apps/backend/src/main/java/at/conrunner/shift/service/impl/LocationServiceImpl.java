package at.conrunner.shift.service.impl;

import org.springframework.transaction.annotation.Transactional;
import at.conrunner.shift.audit.EventManagementAudit;
import java.util.Collection;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import at.conrunner.shift.entity.Location;
import at.conrunner.shift.lib.event.events.LocationEvent;
import at.conrunner.platform.exception.BadRequestException;
import at.conrunner.identity.AdminOnly;
import at.conrunner.shift.dao.EventDao;
import at.conrunner.shift.dao.LocationDao;
import at.conrunner.shift.dto.location.LocationDto;
import at.conrunner.shift.dto.location.LocationModificationDto;
import at.conrunner.shift.mapper.LocationMapper;
import at.conrunner.shift.service.LocationService;
import at.conrunner.shift.util.SecurityHelper;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {
    private final EventManagementAudit eventManagementAudit;
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
    @Transactional
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

        eventManagementAudit.record("location", "created", newLocation.getId(), null, eventManagementAudit.snapshot(newLocation));
        publisher.publishEvent(LocationEvent.locationCreated(newLocation));
        return LocationMapper.toLocationDto(newLocation);
    }


    @Override
    @AdminOnly
    @Transactional
    public @NonNull LocationDto updateLocation(long locationId, @NonNull LocationModificationDto modificationDto) {
        var location = locationDao.getById(locationId);

        if (location.isReadOnly()) {
            throw new BadRequestException("Cannot modify read-only location");
        }

        validateNameUniquenessInEvent(location.getEvent().getId(), modificationDto.getName(), locationId);

        var before = eventManagementAudit.snapshot(location);
        location.setName(modificationDto.getName());
        location.setDescription(modificationDto.getDescription());
        location.setUrl(modificationDto.getUrl());

        location = locationDao.save(location);

        eventManagementAudit.record("location", "updated", location.getId(), before, eventManagementAudit.snapshot(location));
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
    @Transactional
    public void deleteLocation(long locationId) {
        var location = locationDao.getById(locationId);

        if (location.isReadOnly()) {
            throw new BadRequestException("Cannot modify read-only location");
        }

        eventManagementAudit.record("location", "deleted", location.getId(), eventManagementAudit.snapshot(location), null);
        var locationEvent = LocationEvent.locationDeleted(location);
        locationDao.delete(location);
        publisher.publishEvent(locationEvent);
    }
}

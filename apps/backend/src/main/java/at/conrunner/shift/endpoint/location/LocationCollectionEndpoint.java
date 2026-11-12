package at.conrunner.shift.endpoint.location;

import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.conrunner.shift.lib.util.ConvertUtil;
import at.conrunner.shift.dto.location.LocationDto;
import at.conrunner.shift.dto.location.LocationModificationDto;
import at.conrunner.shift.service.LocationService;

@Tag(
    name = "location-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/events/{eventId}/locations", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LocationCollectionEndpoint {
    private final LocationService locationService;

    @GetMapping()
    @Operation(
        operationId = "getAllLocationsForEvent",
        description = "Find all locations for a specific event"
    )
    public Collection<LocationDto> getAllLocationsForEvent(@PathVariable String eventId) {
        return locationService.getAllLocationsForEvent(ConvertUtil.idToLong(eventId));
    }

    @PostMapping()
    @Operation(
        operationId = "createLocation",
        description = "Create a new location for a specific event"
    )
    public LocationDto createLocation(@PathVariable String eventId, @RequestBody @Valid LocationModificationDto modificationDto) {
        return locationService.createLocation(ConvertUtil.idToLong(eventId), modificationDto);
    }
}

package at.control.shiftcontrol.shift.endpoint.location;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.shiftcontrol.lib.util.ConvertUtil;
import at.shiftcontrol.shiftservice.dto.location.LocationDto;
import at.shiftcontrol.shiftservice.dto.location.LocationModificationDto;
import at.shiftcontrol.shiftservice.service.LocationService;

@Tag(
    name = "location-endpoint"
)
@Slf4j
@RestController
@RequestMapping(value = "api/v1/locations/{locationId}", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LocationItemEndpoint {
    private final LocationService locationService;

    @GetMapping()
    @Operation(
        operationId = "getLocation",
        description = "Find a location by its id"
    )
    public LocationDto getLocation(@PathVariable String locationId) {
        return locationService.getLocation(ConvertUtil.idToLong(locationId));
    }

    @PutMapping()
    @Operation(
        operationId = "updateLocation",
        description = "Update a location by its id"
    )
    public LocationDto updateLocation(@PathVariable String locationId, @RequestBody @Valid LocationModificationDto modificationDto) {
        return locationService.updateLocation(ConvertUtil.idToLong(locationId), modificationDto);
    }

    @DeleteMapping()
    @Operation(
        operationId = "deleteLocation",
        description = "Delete a location by its id"
    )
    public void deleteLocation(@PathVariable String locationId) {
        locationService.deleteLocation(ConvertUtil.idToLong(locationId));
    }
}

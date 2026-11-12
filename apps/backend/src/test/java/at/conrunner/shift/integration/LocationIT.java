package at.conrunner.shift.integration;

import static org.assertj.core.api.Assertions.assertThat;

import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.Location;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.type.LockStatus;
import at.conrunner.shift.auth.UserAttributeProvider;
import at.conrunner.shift.dto.location.LocationDto;
import at.conrunner.shift.dto.location.LocationModificationDto;
import at.conrunner.shift.integration.config.RestITBase;
import at.conrunner.shift.repo.EventRepository;
import at.conrunner.shift.repo.LocationRepository;
import at.conrunner.shift.repo.ShiftPlanRepository;
import at.conrunner.shift.repo.VolunteerRepository;
import io.restassured.http.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;

class LocationIT extends RestITBase {
    private static final String LOCATION_PATH = "/locations";
    private static final String EVENT_LOCATION_PATH = "/events/%d/locations";

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    VolunteerRepository volunteerRepository;

    @Autowired
    ShiftPlanRepository shiftPlanRepository;

    @Autowired
    UserAttributeProvider userAttributeProvider;

    private Event eventA, eventB, eventC;

    private Location locationA, locationB, locationC;

    private ShiftPlan shiftPlanA;

    private Volunteer volunteerA;

    @BeforeEach
    void setUp() {

        createEvents();
        createLocations();
        createShiftPlans();
        createVolunteers();
    }

    private void createEvents() {
        var events = new ArrayList<Event>();

        var startTimeA = LocalDateTime.of(2024, 1, 1, 10, 0).toInstant(ZoneOffset.UTC);
        var endTimeA = LocalDateTime.of(2024, 1, 1, 20, 0).toInstant(ZoneOffset.UTC);

        eventA = Event.builder()
            .name("EventA")
            .shortDescription("ShortDescA")
            .longDescription("This is a description for EventA")
            .startTime(startTimeA)
            .endTime(endTimeA)
            .build();
        events.add(eventA);

        var startTimeB = LocalDateTime.of(2024, 2, 1, 11, 0).toInstant(ZoneOffset.UTC);
        var endTimeB = LocalDateTime.of(2024, 2, 3, 18, 0).toInstant(ZoneOffset.UTC);

        eventB = Event.builder()
            .name("EventB")
            .shortDescription("ShortDescB")
            .longDescription("This is a description for EventB")
            .startTime(startTimeB)
            .endTime(endTimeB)
            .build();
        events.add(eventB);

        eventRepository.saveAll(events);

        assertAll(
            () -> assertThat(eventA.getId()).isGreaterThan(0),
            () -> assertThat(eventB.getId()).isGreaterThan(0),
            () -> assertThat(eventRepository.existsById(eventA.getId())).isTrue(),
            () -> assertThat(eventRepository.existsById(eventB.getId())).isTrue()
        );
    }

    private void createLocations() {
        var locations = new ArrayList<Location>();

        locationA = Location.builder()
            .name("LocationA")
            .event(eventA)
            .description("AddressA")
            .url("http://locationa.com")
            .readOnly(false)
            .build();
        locations.add(locationA);

        locationB = Location.builder()
            .name("LocationB")
            .event(eventA)
            .description("AddressB")
            .url("http://locationb.com")
            .readOnly(true)
            .build();
        locations.add(locationB);

        locationC = Location.builder()
            .name("LocationC")
            .event(eventB)
            .description("AddressC")
            .url("http://locationc.com")
            .readOnly(false)
            .build();
        locations.add(locationC);

        locationRepository.saveAll(locations);

        assertAll(
            () -> assertThat(locationA.getId()).isGreaterThan(0),
            () -> assertThat(locationB.getId()).isGreaterThan(0),
            () -> assertThat(locationC.getId()).isGreaterThan(0),
            () -> assertThat(locationRepository.existsById(locationA.getId())).isTrue(),
            () -> assertThat(locationRepository.existsById(locationB.getId())).isTrue(),
            () -> assertThat(locationRepository.existsById(locationC.getId())).isTrue()
        );
    }

    private void createShiftPlans() {
        shiftPlanA = ShiftPlan.builder()
            .name("ShiftPlanA")
            .event(eventA)
            .lockStatus(LockStatus.SELF_SIGNUP)
            .build();

        shiftPlanRepository.save(shiftPlanA);

        assertAll(
            () -> assertThat(shiftPlanA.getId()).isGreaterThan(0),
            () -> assertThat(shiftPlanRepository.existsById(shiftPlanA.getId())).isTrue()
        );
    }

    private void createVolunteers() {
        volunteerA = Volunteer.builder()
            .id(java.util.UUID.fromString("00422160-290c-5d94-a18d-3d652c6f6e37"))
            .volunteeringPlans(List.of(shiftPlanA))
            .build();
        volunteerRepository.save(volunteerA);

        shiftPlanA.setPlanVolunteers(List.of(volunteerA));
        shiftPlanRepository.save(shiftPlanA);

        userAttributeProvider.invalidateUserCache(volunteerA.getId());
    }

    @AfterEach
    void tearDown() {


}

    @Test
    void findEventByNonExistingIdReturnsNotFound() {
        doRequestAsAssignedAndAssertMessage(Method.GET, LOCATION_PATH + "/999", "", HttpStatus.NOT_FOUND.value(), "Location not found.", true, "123abc");
    }

    @Test
    void findEventByExistingIdReturnsLocationSuccessfully() {
        var response = getRequestAsAssigned(LOCATION_PATH + "/" + locationA.getId(), LocationDto.class, volunteerA.getId());

        assertThat(response).isNotNull();
        assertAll(
            () -> assertThat(response.getId()).isEqualTo(String.valueOf(locationA.getId())),
            () -> assertThat(response.getName()).isEqualTo(locationA.getName()),
            () -> assertThat(response.getDescription()).isEqualTo(locationA.getDescription()),
            () -> assertThat(response.getUrl()).isEqualTo(locationA.getUrl())
        );
    }

    @Test
    void getAllLocationsForEventReturnsLocationsSuccessfully() {
        var response = getRequestListAsAssigned(EVENT_LOCATION_PATH.formatted(eventA.getId()), LocationDto.class, volunteerA.getId());

        assertThat(response).isNotNull();
        assertAll(
            () -> assertThat(response).hasSize(2),
            () -> assertThat(response.stream().anyMatch(loc -> loc.getId().equals(String.valueOf(locationA.getId())))).isTrue(),
            () -> assertThat(response.stream().anyMatch(loc -> loc.getId().equals(String.valueOf(locationB.getId())))).isTrue()
        );
    }

    @Test
    void getAllLocationsForNonExistingEventReturnsNotFound() {
        doRequestAsAssignedAndAssertMessage(Method.GET, EVENT_LOCATION_PATH.formatted(999), "", HttpStatus.NOT_FOUND.value(), "Event not found.", true, "123abc");
    }

    @Test
    void createLocationAsNonAdminReturnsForbidden() {
        var newLocationModificationDto = LocationModificationDto.builder()
            .name("New Location")
            .description("New Address")
            .url("http://newlocation.com")
            .build();

        doRequestAndAssertAndAssertStausCode(Method.POST, EVENT_LOCATION_PATH.formatted(eventA.getId()), newLocationModificationDto, HttpStatus.FORBIDDEN.value());
    }

    @Test
    void createLocationAsAdminForNonExistingEventReturnsNotFound() {
        var newLocationModificationDto = LocationModificationDto.builder()
            .name("New Location")
            .description("New Address")
            .url("http://newlocation.com")
            .build();

        doRequestAsAdminAndAssertMessage(Method.POST, EVENT_LOCATION_PATH.formatted(999), newLocationModificationDto, HttpStatus.NOT_FOUND.value(),
            "Event not found.", true);
    }

    @Test
    void createLocationAsAdminForExistingEventReturnsLocationSuccessfully() {
        var newLocationModificationDto = LocationModificationDto.builder()
            .name("New Location")
            .description("New Address")
            .url("http://newlocation.com")
            .build();

        var response = postRequestAsAdmin(EVENT_LOCATION_PATH.formatted(eventA.getId()), newLocationModificationDto, LocationDto.class);

        assertAll(
            () -> assertThat(response).isNotNull(),
            () -> assertThat(response.getId()).isNotNull(),
            () -> assertThat(response.getName()).isEqualTo(newLocationModificationDto.getName()),
            () -> assertThat(response.getDescription()).isEqualTo(newLocationModificationDto.getDescription()),
            () -> assertThat(response.getUrl()).isEqualTo(newLocationModificationDto.getUrl()),
            () -> assertThat(response.isReadOnly()).isFalse()
        );
    }

    @Test
    void createLocationAsAdminWithDuplicateNameReturnsBadRequest() {
        var newLocationModificationDto = LocationModificationDto.builder()
            .name(locationA.getName())
            .description("New Address")
            .url("http://newlocation.com")
            .build();

        doRequestAsAdminAndAssertMessage(Method.POST, EVENT_LOCATION_PATH.formatted(eventA.getId()), newLocationModificationDto, 400,
            "Location name must be unique within an event", true);
    }

    @Test
    void updateLocationAsNonAdminReturnsForbidden() {
        var updateLocationModificationDto = LocationModificationDto.builder()
            .name("Updated Location")
            .description("Updated Address")
            .url("http://updatedlocation.com")
            .build();

        doRequestAndAssertAndAssertStausCode(Method.PUT, LOCATION_PATH + "/" + locationA.getId(), updateLocationModificationDto, HttpStatus.FORBIDDEN.value());
    }

    @Test
    void updateNonExistingLocationAsAdminReturnsNotFound() {
        var updateLocationModificationDto = LocationModificationDto.builder()
            .name("Updated Location")
            .description("Updated Address")
            .url("http://updatedlocation.com")
            .build();

        doRequestAsAdminAndAssertMessage(Method.PUT, LOCATION_PATH + "/999", updateLocationModificationDto, HttpStatus.NOT_FOUND.value(),
            "Location not found.", true);
    }

    @Test
    void updateReadOnlyLocationAsAdminReturnsBadRequest() {
        var updateLocationModificationDto = LocationModificationDto.builder()
            .name("Updated Location")
            .description("Updated Address")
            .url("http://updatedlocation.com")
            .build();

        doRequestAsAdminAndAssertMessage(Method.PUT, LOCATION_PATH + "/" + locationB.getId(), updateLocationModificationDto, 400,
            "Cannot modify read-only location", true);
    }

    @Test
    void updateLocationAsAdminReturnsLocationSuccessfully() {
        var updateLocationModificationDto = LocationModificationDto.builder()
            .name("Updated Location")
            .description("Updated Address")
            .url("http://updatedlocation.com")
            .build();

        var response = putRequestAsAdmin(LOCATION_PATH + "/" + locationA.getId(), updateLocationModificationDto, LocationDto.class);

        assertAll(
            () -> assertThat(response).isNotNull(),
            () -> assertThat(response.getId()).isEqualTo(String.valueOf(locationA.getId())),
            () -> assertThat(response.getName()).isEqualTo(updateLocationModificationDto.getName()),
            () -> assertThat(response.getDescription()).isEqualTo(updateLocationModificationDto.getDescription()),
            () -> assertThat(response.getUrl()).isEqualTo(updateLocationModificationDto.getUrl()),
            () -> assertThat(response.isReadOnly()).isFalse()
        );
    }

    @Test
    void deleteLocationAsNonAdminReturnsForbidden() {
        doRequestAndAssertAndAssertStausCode(Method.DELETE, LOCATION_PATH + "/" + locationA.getId(), "", HttpStatus.FORBIDDEN.value());
    }

    @Test
    void deleteNonExistingLocationAsAdminReturnsNotFound() {
        doRequestAsAdminAndAssertMessage(Method.DELETE, LOCATION_PATH + "/999", "", HttpStatus.NOT_FOUND.value(),
            "Location not found.", true);
    }

    @Test
    void deleteReadOnlyLocationAsAdminReturnsBadRequest() {
        doRequestAsAdminAndAssertMessage(Method.DELETE, LOCATION_PATH + "/" + locationB.getId(), "", 400,
            "Cannot modify read-only location", true);
    }

    @Test
    void deleteLocationAsAdminDeletesLocationSuccessfully() {
        deleteRequestAsAdmin(LOCATION_PATH + "/" + locationA.getId());

        assertThat(locationRepository.existsById(locationA.getId())).isFalse();
    }


}

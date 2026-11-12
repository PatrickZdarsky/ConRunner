package at.conrunner.shift.util;

import at.conrunner.shift.entity.Activity;
import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.AssignmentSwitchRequest;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.Location;
import at.conrunner.shift.entity.PositionConstraint;
import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.entity.Role;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.TimeConstraint;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.dto.positionslot.PositionSlotRequestDto;
import at.conrunner.shift.dto.userprofile.AccountInfoDto;
import at.conrunner.shift.dto.userprofile.UserProfileDto;
import at.conrunner.shift.dto.userprofile.VolunteerDto;
import at.conrunner.shift.repo.ActivityRepository;
import at.conrunner.shift.repo.AssignmentRepository;
import at.conrunner.shift.repo.AssignmentSwitchRequestRepository;
import at.conrunner.shift.repo.EventRepository;
import at.conrunner.shift.repo.LocationRepository;
import at.conrunner.shift.repo.PositionConstraintRepository;
import at.conrunner.shift.repo.PositionSlotRepository;
import at.conrunner.shift.repo.ShiftPlanRepository;
import at.conrunner.shift.repo.ShiftRepository;
import at.conrunner.shift.repo.TimeConstraintRepository;
import at.conrunner.shift.repo.VolunteerRepository;
import at.conrunner.shift.repo.role.RoleRepository;
import at.conrunner.shift.service.rewardpoints.RewardPointsCalculator;
import at.conrunner.identity.User;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class TestEntityFactory {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ShiftPlanRepository shiftPlanRepository;
    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private TimeConstraintRepository timeConstraintRepository;
    @Autowired
    private VolunteerRepository volunteerRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PositionSlotRepository positionSlotRepository;
    @Autowired
    private PositionConstraintRepository positionConstraintRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private AssignmentSwitchRequestRepository assignmentSwitchRequestRepository;

    @Autowired
    private RewardPointsCalculator rewardPointsCalculator;

    public Event createPersistedEvent() {
        Event event = Event.builder()
            .name("eventName")
            .startTime(Instant.now())
            .endTime(Instant.now())
            .build();
        return eventRepository.save(event);
    }

    public ShiftPlan createPersistedShiftPlan() {
        throw new NotImplementedException();
    }

    public Shift createPersistedShift() {
        throw new NotImplementedException();
    }

    public Activity createPersistedActivity() {
        Activity activity = Activity.builder()
            .name("activityName")
            .description("activityDescription")
            .event(createPersistedEvent())
            .startTime(Instant.now())
            .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
            .build();
        return activityRepository.save(activity);
    }

    public Location createPersistedLocation() {
        throw new NotImplementedException();
    }

    public TimeConstraint createPersistedAttendanceTimeConstraint() {
        throw new NotImplementedException();
    }

    public Volunteer createPersistedVolunteer() {
        throw new NotImplementedException();
    }

    public Role createPersistedRole() {
        Role role = Role.builder()
            .name("testRole")
            .description("This is a role description.")
            .build();
        return roleRepository.save(role);
    }

    public PositionSlot createPersistedPositionSlot() {
        throw new NotImplementedException();
    }

    public PositionConstraint createPersistedPositionConstraint() {
        throw new NotImplementedException();
    }

    public Assignment createPersistedAssignment() {
        throw new NotImplementedException();
    }

    public AssignmentSwitchRequest createPersistedAssignmentSwitchRequest() {
        throw new NotImplementedException();
    }

    public UserProfileDto getUserProfileDtoWithId(String userId) {
        UserProfileDto profile = new UserProfileDto();
        VolunteerDto volunteer = new VolunteerDto(
            java.util.UUID.fromString(userId),
            "assigned-" + userId,
            "first name",
            "https://cdn.example.test/profiles/" + userId + ".png"
        );
        AccountInfoDto info = new AccountInfoDto(
            volunteer,
            "Test Username",
            "mail@mail.com",
            "https://cdn.example.test/profiles/" + userId + ".png",
            false
        );
        profile.setAccount(info);
        return profile;
    }

    public User getUserInfoWithId(String id) {
        return User.builder().id(java.util.UUID.fromString(id)).username("assigned-" + id)
            .email(id + "@example.com").avatarUrl("https://cdn.example.test/profiles/" + id + ".png").build();
    }

    public User getAdminUserInfoWithId(String id) {
        return getUserInfoWithId(id);
    }

    public PositionSlotRequestDto getPositionSlotRequestDto(long positionSlotId) {
        PositionSlot positionSlot = positionSlotRepository.getReferenceById(positionSlotId);
        String hash = rewardPointsCalculator.calculatePointsConfigHash(positionSlot);
        return new PositionSlotRequestDto(hash);
    }
}

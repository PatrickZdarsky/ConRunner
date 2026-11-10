package at.control.shiftcontrol.shift.util;

import at.control.shiftcontrol.shift.entity.Activity;
import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;
import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.Location;
import at.control.shiftcontrol.shift.entity.PositionConstraint;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Role;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.TimeConstraint;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotRequestDto;
import at.control.shiftcontrol.shift.dto.userprofile.AccountInfoDto;
import at.control.shiftcontrol.shift.dto.userprofile.UserProfileDto;
import at.control.shiftcontrol.shift.dto.userprofile.VolunteerDto;
import at.control.shiftcontrol.shift.repo.ActivityRepository;
import at.control.shiftcontrol.shift.repo.AssignmentRepository;
import at.control.shiftcontrol.shift.repo.AssignmentSwitchRequestRepository;
import at.control.shiftcontrol.shift.repo.EventRepository;
import at.control.shiftcontrol.shift.repo.LocationRepository;
import at.control.shiftcontrol.shift.repo.PositionConstraintRepository;
import at.control.shiftcontrol.shift.repo.PositionSlotRepository;
import at.control.shiftcontrol.shift.repo.ShiftPlanRepository;
import at.control.shiftcontrol.shift.repo.ShiftRepository;
import at.control.shiftcontrol.shift.repo.TimeConstraintRepository;
import at.control.shiftcontrol.shift.repo.VolunteerRepository;
import at.control.shiftcontrol.shift.repo.role.RoleRepository;
import at.control.shiftcontrol.shift.service.rewardpoints.RewardPointsCalculator;
import at.control.shiftcontrol.identity.User;
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
        // TODO add other fields
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

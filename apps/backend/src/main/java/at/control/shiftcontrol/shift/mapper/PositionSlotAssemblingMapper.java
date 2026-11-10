package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;
import at.control.shiftcontrol.shift.lib.type.PositionSignupState;
import at.control.shiftcontrol.shift.auth.CurrentShiftActor;
import at.control.shiftcontrol.shift.dao.AssignmentDao;
import at.control.shiftcontrol.shift.dao.AssignmentSwitchRequestDao;
import at.control.shiftcontrol.shift.dao.PositionSlotDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.assignment.AssignmentDto;
import at.control.shiftcontrol.shift.dto.positionslot.PositionSlotDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.RewardPointsDto;
import at.control.shiftcontrol.shift.dto.trade.TradeCandidatesDto;
import at.control.shiftcontrol.shift.service.EligibilityService;
import at.control.shiftcontrol.identity.UserService;

@RequiredArgsConstructor
@Service
public class PositionSlotAssemblingMapper {
    private final EligibilityService eligibilityService;
    private final CurrentShiftActor currentActor;
    private final VolunteerDao volunteerDao;
    private final AssignmentDao assignmentDao;
    private final PositionSlotDao positionSlotDao;
    private final AssignmentAssemblingMapper assignmentAssemblingMapper;
    private final RewardPointsAssemblingMapper rewardPointsAssemblingMapper;
    private final TradeMapper tradeMapper;
    private final UserService userService;
    private final AssignmentSwitchRequestDao assignmentSwitchRequestDao;

    public PositionSlotDto assemble(@NonNull PositionSlot positionSlot) {
        var actor = currentActor.get();
        var volunteer = volunteerDao.getById(actor.requireUserId());
        var preferenceValue = positionSlotDao.getPreference(volunteer.getId().toString(), positionSlot.getId());
        var rewardPointsDto = rewardPointsAssemblingMapper.toDto(positionSlot);

        // Offerer: requested slot (requestedAssignment is in this slot)
        var requestedTradesAsOfferer = assignmentSwitchRequestDao.findOpenTradesForRequestedPositionAndOfferingUser(
            positionSlot.getId(), volunteer.getId().toString()
        );

        // Owner: requested slot (requestedAssignment belongs to me)
        var incomingRequestsForUserInThisSlot = filterIncomingTradesForUser(positionSlot.getAssignments(), volunteer.getId().toString());

        var requestedTradesForUser = Stream.concat(
                requestedTradesAsOfferer == null ? Stream.empty() : requestedTradesAsOfferer.stream(),
                incomingRequestsForUserInThisSlot.stream()
            )
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        // Offerer: offered slot (offeringAssignment belongs to me)
        var offeredTradesAsOfferer = filterOfferedTradesForUser(positionSlot.getAssignments(), volunteer.getId().toString());

        // Owner: offered slot (offeringAssignment is in this slot)
        var offeredTradesAsRequestedUser = assignmentSwitchRequestDao
            .findOpenTradesForOfferingPositionAndRequestedOwner(positionSlot.getId(), volunteer.getId().toString());

        var offeredTradesForUser = Stream.concat(
                offeredTradesAsOfferer.stream(),
                offeredTradesAsRequestedUser == null ? Stream.empty() : offeredTradesAsRequestedUser.stream()
            )
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        var signupState = eligibilityService.getSignupStateForPositionSlot(positionSlot, volunteer);
        if (signupState != PositionSignupState.NOT_ELIGIBLE
            && !actor.isVolunteerInPlan(positionSlot.getShift().getShiftPlan().getId())) {
            signupState = PositionSignupState.NOT_ELIGIBLE;
        }

        return toDto(
            positionSlot,
            signupState,
            offeredTradesForUser,
            requestedTradesForUser,
            preferenceValue,
            rewardPointsDto
        );
    }

    public Collection<PositionSlotDto> assemble(@NonNull Collection<PositionSlot> positionSlots) {
        return positionSlots.stream().map(this::assemble).toList();
    }

    private Collection<AssignmentSwitchRequest> filterIncomingTradesForUser(Collection<Assignment> assignments, String userId) {
        if (assignments == null || assignments.isEmpty()) {
            return Collections.emptyList();
        }
        return assignments.stream()
            .filter(a -> a.getAssignedVolunteer() != null && a.getAssignedVolunteer().getId().toString().equals(userId))
            .flatMap(a -> a.getIncomingSwitchRequests() == null ? Stream.empty() : a.getIncomingSwitchRequests().stream())
            .toList();
    }

    private Collection<AssignmentSwitchRequest> filterOfferedTradesForUser(Collection<Assignment> assignments, String userId) {
        if (assignments == null || assignments.isEmpty()) {
            return Collections.emptyList();
        }
        return assignments.stream()
            .filter(a -> a.getAssignedVolunteer() != null && a.getAssignedVolunteer().getId().toString().equals(userId))
            .flatMap(a -> a.getOutgoingSwitchRequests() == null ? Stream.empty() : a.getOutgoingSwitchRequests().stream())
            .toList();
    }

    private PositionSlotDto toDto(@NonNull PositionSlot positionSlot, @NonNull PositionSignupState positionSignupState,
                                  Collection<AssignmentSwitchRequest> offeredTradesForUser, Collection<AssignmentSwitchRequest> requestedTradesForUser,
                                  int preferenceValue, RewardPointsDto rewardPointsDto) {
        Collection<AssignmentDto> assignmentDtos;
        Collection<AssignmentDto> auctionDtos;

        var activeAssignments = assignmentDao.getActiveAssignmentsOfSlot(positionSlot.getId());
        if (activeAssignments == null) {
            assignmentDtos = null;
            auctionDtos = null;
        } else {
            assignmentDtos = assignmentAssemblingMapper.assemble(activeAssignments);
            auctionDtos = assignmentAssemblingMapper.assemble(activeAssignments.stream()
                .filter(a -> AssignmentStatus.ACTIVE_AUCTION_STATES.contains(a.getStatus()))
                .toList()); // get open auctions for this slot
        }
        return new PositionSlotDto(
            String.valueOf(positionSlot.getId()),
            positionSlot.getName(),
            positionSlot.getDescription(),
            positionSlot.isSkipAutoAssignment(),
            String.valueOf(positionSlot.getShift().getId()),
            positionSlot.getRole() == null ? null : RoleMapper.toRoleDto(positionSlot.getRole()),
            assignmentDtos,
            positionSlot.getDesiredVolunteerCount(),
            positionSignupState,
            tradeMapper.toTradeInfoDto(offeredTradesForUser),
            tradeMapper.toTradeInfoDto(requestedTradesForUser),
            auctionDtos,
            preferenceValue,
            positionSlot.getShift().getShiftPlan().getLockStatus(),
            rewardPointsDto);
    }

    public TradeCandidatesDto tradeCandidatesDto(@NonNull PositionSlot positionSlot, Collection<Volunteer> volunteers) {
        var shift = positionSlot.getShift();
        var users = userService.getUsers(volunteers.stream().map(Volunteer::getId).toList());
        return new TradeCandidatesDto(
            assemble(positionSlot),
            shift.getName(),
            shift.getStartTime(),
            VolunteerAssemblingMapper.toDtoFromUser(users)
        );
    }
}

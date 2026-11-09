package at.control.shiftcontrol.shift.service.rewardpoints.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.common.UniqueCodeGenerator;
import at.shiftcontrol.lib.entity.Assignment;
import at.shiftcontrol.lib.entity.PositionSlot;
import at.shiftcontrol.lib.entity.RewardPointsShareToken;
import at.shiftcontrol.lib.entity.Volunteer;
import at.shiftcontrol.lib.event.events.RewardPointsShareTokenEvent;
import at.shiftcontrol.lib.exception.BadRequestException;
import at.shiftcontrol.lib.exception.ConflictException;
import at.shiftcontrol.shiftservice.annotation.AdminOnly;
import at.shiftcontrol.shiftservice.dao.EventDao;
import at.shiftcontrol.shiftservice.dao.RewardPointsShareTokenDao;
import at.shiftcontrol.shiftservice.dao.RewardPointsTransactionDao;
import at.shiftcontrol.shiftservice.dao.userprofile.VolunteerDao;
import at.shiftcontrol.shiftservice.dto.rewardpoints.RewardPointsExportDto;
import at.shiftcontrol.shiftservice.dto.rewardpoints.RewardPointsShareTokenCreateRequestDto;
import at.shiftcontrol.shiftservice.dto.rewardpoints.RewardPointsShareTokenDto;
import at.shiftcontrol.shiftservice.dto.rewardpoints.RewardPointsSnapshotDto;
import at.shiftcontrol.shiftservice.dto.rewardpoints.VolunteerPointsDto;
import at.shiftcontrol.shiftservice.mapper.EventMapper;
import at.shiftcontrol.shiftservice.mapper.RewardPointsMapper;
import at.shiftcontrol.shiftservice.service.rewardpoints.RewardPointsCalculator;
import at.shiftcontrol.shiftservice.service.rewardpoints.RewardPointsLedgerService;
import at.shiftcontrol.shiftservice.service.rewardpoints.RewardPointsService;
import at.shiftcontrol.shiftservice.userdirectory.UserInfo;
import at.shiftcontrol.shiftservice.userdirectory.UserLookupService;

@Service
@RequiredArgsConstructor
@Primary
public class RewardPointsServiceImpl implements RewardPointsService {
    private final RewardPointsCalculator calculator;
    private final RewardPointsLedgerService ledgerService;

    private final RewardPointsShareTokenDao rewardPointsShareTokenDao;
    private final EventDao eventDao;
    private final VolunteerDao volunteerDao;
    private final RewardPointsTransactionDao rewardPointsTransactionDao;

    private final UserLookupService userDirectoryService;
    private final ApplicationEventPublisher publisher;

    private final UniqueCodeGenerator uniqueCodeGenerator;

    private static final String SHARE_TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SHARE_TOKEN_LENGTH = 10;
    private static final int MAX_SHARE_TOKEN_GENERATION_ATTEMPTS = 10;

    @Override
    @Transactional
    public void onAssignmentAccepted(Assignment assignment) throws ConflictException {
        // no need to validate hash, because not available when planner accepts signup request
        onAssignmentJoined(assignment);
    }

    @Override
    @Transactional
    public void onAssignmentCreated(@NonNull Assignment assignment, @NonNull String acceptedRewardPointsHash) throws ConflictException {
        PositionSlot slot = assignment.getPositionSlot();
        validateHash(slot, acceptedRewardPointsHash);

        onAssignmentJoined(assignment);
    }

    private void onAssignmentJoined(@NonNull Assignment assignment) {
        RewardPointsSnapshotDto snapshot = calculator.calculateForAssignment(assignment.getPositionSlot());

        String sourceKey = sourceKeyJoin(assignment.getPositionSlot().getId(), assignment.getAssignedVolunteer().getId());

        var result = ledgerService.bookEarn(RewardPointsMapper.toRewardPointsTransactionDto(
            assignment,
            snapshot.rewardPoints(),
            sourceKey,
            snapshot.metadata()
        ));

        if (result.created()) {
            // only set if booking was created
            assignment.setAcceptedRewardPoints(result.transaction().getPoints());
        }
    }

    private String sourceKeyJoin(long slotId, String volunteerId) {
        return "JOIN:" + slotId + ":" + volunteerId;
    }


    @Override
    @Transactional
    public void onAssignmentRemoved(@NonNull Assignment assignment) {
        PositionSlot slot = assignment.getPositionSlot();
        int pointsSnapshot = assignment.getAcceptedRewardPoints();

        String sourceKey = sourceKeyLeave(slot.getId(), assignment.getAssignedVolunteer().getId());

        var result = ledgerService.bookReversal(RewardPointsMapper.toRewardPointsTransactionDto(
            assignment,
            pointsSnapshot,
            sourceKey,
            null
        ));

        if (result.created()) {
            // only clear if booking was created
            assignment.setAcceptedRewardPoints(0);
        }
    }

    private String sourceKeyLeave(long slotId, String volunteerId) {
        return "LEAVE:" + slotId + ":" + volunteerId;
    }

    @Override
    @Transactional
    public void onAssignmentReassignedAuction(@NonNull Assignment oldAssignment, @NonNull Assignment newAssignment,
                                              @NonNull String acceptedRewardPointsHash) throws ConflictException {
        PositionSlot slot = oldAssignment.getPositionSlot();
        // reverse old assignment
        int oldPointsSnapshot = oldAssignment.getAcceptedRewardPoints();

        String reversalKey = sourceKeyReassignReversal(
            slot.getId(),
            oldAssignment.getAssignedVolunteer().getId(),
            newAssignment.getAssignedVolunteer().getId()
        );

        var unassignmentResult = ledgerService.bookReversal(RewardPointsMapper.toRewardPointsTransactionDto(
            oldAssignment,
            oldPointsSnapshot,
            reversalKey,
            null
        ));

        if (unassignmentResult.created()) {
            // only clear if booking was created
            oldAssignment.setAcceptedRewardPoints(0);
        }

        // re-calculate + earn for new assignment
        validateHash(slot, acceptedRewardPointsHash);
        RewardPointsSnapshotDto newSnapshot = calculator.calculateForAssignment(slot);

        String earnKey = sourceKeyReassignEarn(
            slot.getId(),
            oldAssignment.getAssignedVolunteer().getId(),
            newAssignment.getAssignedVolunteer().getId()
        );

        var assignmentResult = ledgerService.bookEarn(RewardPointsMapper.toRewardPointsTransactionDto(
            newAssignment,
            newSnapshot.rewardPoints(),
            earnKey,
            newSnapshot.metadata()
        ));

        if (assignmentResult.created()) {
            // only set if booking for new assigment was created
            newAssignment.setAcceptedRewardPoints(newSnapshot.rewardPoints());
        }
    }

    @Override
    @Transactional
    public void onAssignmentReassignedTrade(@NonNull Assignment offeringAssignment, @NonNull Assignment requestedAssignment) throws ConflictException {
        int offeringPoints = offeringAssignment.getAcceptedRewardPoints();
        int requestedPoints = requestedAssignment.getAcceptedRewardPoints();

        // create ledger entries for both reversals and earnings for both assignments
        String reversalKeyOffering = sourceKeyReassignReversal(
            offeringAssignment.getPositionSlot().getId(),
            requestedAssignment.getAssignedVolunteer().getId(),
            offeringAssignment.getAssignedVolunteer().getId()
        );

        // take away old points
        ledgerService.bookReversal(RewardPointsMapper.toRewardPointsTransactionDto(
            offeringAssignment,
            requestedPoints,
            reversalKeyOffering,
            Map.of("note", "reversal due to trade")
        ));

        String earnKeyOffering = sourceKeyReassignEarn(
            offeringAssignment.getPositionSlot().getId(),
            requestedAssignment.getAssignedVolunteer().getId(),
            offeringAssignment.getAssignedVolunteer().getId()
        );

        // give new points
        ledgerService.bookEarn(RewardPointsMapper.toRewardPointsTransactionDto(
            offeringAssignment,
            offeringPoints,
            earnKeyOffering,
            Map.of("note", "earn due to trade")
        ));


        String reversalKeyRequested = sourceKeyReassignReversal(
            requestedAssignment.getPositionSlot().getId(),
            offeringAssignment.getAssignedVolunteer().getId(),
            requestedAssignment.getAssignedVolunteer().getId()
        );

        // take away old points
        ledgerService.bookReversal(RewardPointsMapper.toRewardPointsTransactionDto(
            requestedAssignment,
            offeringPoints,
            reversalKeyRequested,
            Map.of("note", "reversal due to trade")
        ));

        String earnKeyRequested = sourceKeyReassignEarn(
            requestedAssignment.getPositionSlot().getId(),
            offeringAssignment.getAssignedVolunteer().getId(),
            requestedAssignment.getAssignedVolunteer().getId()
        );

        ledgerService.bookEarn(RewardPointsMapper.toRewardPointsTransactionDto(
            requestedAssignment,
            requestedPoints,
            earnKeyRequested,
            Map.of("note", "earn due to trade")
        ));
    }

    private void validateHash(@NonNull PositionSlot slot, @NonNull String acceptedRewardPointsHash) throws ConflictException {
        String currentHash = calculator.calculatePointsConfigHash(slot);

        if (!currentHash.equals(acceptedRewardPointsHash)) {
            throw new ConflictException("Reward points configuration has changed since volunteer accepted assignment");
        }
    }

    private String sourceKeyReassignReversal(long slotId, String fromVolunteerId, String toVolunteerId) {
        return "REASSIGN:REV:" + slotId + ":" + fromVolunteerId + ":" + toVolunteerId;
    }

    private String sourceKeyReassignEarn(long slotId, String fromVolunteerId, String toVolunteerId) {
        return "REASSIGN:EARN:" + slotId + ":" + fromVolunteerId + ":" + toVolunteerId;
    }

    @Override
    @AdminOnly
    public void manualAdjust(String volunteerId, long eventId, int points, String reason) {
        if (points == 0) {
            return; // no-op
        }

        String sourceKey = "MANUAL:" + UUID.randomUUID();

        Map<String, Object> metadata = new HashMap<>();
        if (reason != null) {
            metadata.put("reason", reason);
        }

        ledgerService.bookManualAdjust(RewardPointsMapper.toRewardPointsTransactionDto(
            volunteerId,
            eventId,
            null,
            null,
            points,
            sourceKey,
            metadata
        ));
    }

    @Override
    @AdminOnly
    public Collection<RewardPointsShareTokenDto> getAllRewardPointsShareTokens() {
        var tokens = rewardPointsShareTokenDao.findAll();
        return RewardPointsMapper.toRewardPointsShareTokenDto(tokens);
    }

    @Override
    public Collection<RewardPointsExportDto> getRewardPointsWithShareToken(String token) {
        var tokenExists = rewardPointsShareTokenDao.existsByToken(token);

        if (!tokenExists) {
            throw new BadRequestException("Invalid reward points share token");
        }

        return getRewardPointsForAllUsersOverAllEvents();
    }

    public @NonNull Collection<RewardPointsExportDto> getRewardPointsForAllUsersOverAllEvents() {
        var allEvents = eventDao.findAll();

        var exportDtos = new ArrayList<RewardPointsExportDto>();
        for (var event : allEvents) {
            var exportDto = new RewardPointsExportDto();
            exportDto.setEvent(EventMapper.toEventDto(event));
            var isEventFinished = event.getEndTime().isBefore(Instant.now());
            exportDto.setEventFinished(isEventFinished);

            var volunteersOfEvent = volunteerDao.findAllByEvent(event.getId());
            Map<String, UserInfo> usersById = userDirectoryService.getUserByIds(
                volunteersOfEvent.stream().map(Volunteer::getId).toList()
            ).stream().collect(java.util.stream.Collectors.toMap(UserInfo::id, Function.identity()));
            var volunteerPointsDtos = new ArrayList<VolunteerPointsDto>();
            for (var volunteer : volunteersOfEvent) {
                var volunteerPointsDto = new VolunteerPointsDto();
                volunteerPointsDto.setVolunteerId(volunteer.getId());
                var directoryUser = usersById.get(volunteer.getId());
                volunteerPointsDto.setFirstName(directoryUser.firstName());
                volunteerPointsDto.setLastName(directoryUser.lastName());
                volunteerPointsDto.setEmail(directoryUser.email());
                var pointsForEvent = rewardPointsTransactionDao.sumPointsByVolunteerAndEvent(volunteer.getId(), event.getId());
                volunteerPointsDto.setRewardPoints((int) pointsForEvent);

                volunteerPointsDtos.add(volunteerPointsDto);
            }
            exportDto.setVolunteerPoints(volunteerPointsDtos);
            exportDtos.add(exportDto);
        }

        return exportDtos;
    }

    @Override
    @AdminOnly
    public RewardPointsShareTokenDto createRewardPointsShareToken(RewardPointsShareTokenCreateRequestDto requestDto) {
        var tokenCode = callGenerateUniqueCode();

        boolean nameAlreadyExists = rewardPointsShareTokenDao.existsByNameIgnoreCase(requestDto.getName());
        if (nameAlreadyExists) {
            throw new BadRequestException("A share token with the given name already exists");
        }

        var token = RewardPointsShareToken.builder()
            .token(tokenCode)
            .name(requestDto.getName())
            .createdAt(Instant.now())
            .build();

        // Should not happen but just in case we retry once
        try {
            rewardPointsShareTokenDao.save(token);
        } catch (DataIntegrityViolationException e) {
            // fallback once, because code uniqueness might collide under concurrency
            token.setToken(callGenerateUniqueCode());
            rewardPointsShareTokenDao.save(token);
        }

        publisher.publishEvent(RewardPointsShareTokenEvent.shareTokenCreated(token));
        return RewardPointsMapper.toRewardPointsShareTokenDto(token);
    }

    private String callGenerateUniqueCode() {
        return uniqueCodeGenerator.generateUnique(
            SHARE_TOKEN_ALPHABET,
            SHARE_TOKEN_LENGTH,
            MAX_SHARE_TOKEN_GENERATION_ATTEMPTS,
            rewardPointsShareTokenDao::existsByToken
        );
    }

    @Override
    @AdminOnly
    public void deleteRewardPointsShareToken(long id) {
        var token = rewardPointsShareTokenDao.getById(id);

        var rewardPointsShareTokenEvent = RewardPointsShareTokenEvent.shareTokenDeleted(token);
        rewardPointsShareTokenDao.delete(token);
        publisher.publishEvent(rewardPointsShareTokenEvent);
    }
}

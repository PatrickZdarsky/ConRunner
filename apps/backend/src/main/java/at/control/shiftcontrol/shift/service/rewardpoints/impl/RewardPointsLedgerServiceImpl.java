package at.control.shiftcontrol.shift.service.rewardpoints.impl;

import java.util.UUID;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.entity.RewardPointsTransaction;
import at.control.shiftcontrol.shift.lib.event.events.RewardPointTransactionEvent;
import at.control.shiftcontrol.platform.exception.IllegalArgumentException;
import at.control.shiftcontrol.shift.lib.type.RewardPointTransactionType;
import at.control.shiftcontrol.shift.dao.EventDao;
import at.control.shiftcontrol.shift.dao.RewardPointsTransactionDao;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.rewardpoints.BookingResultDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.EventPointsDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.RewardPointsTransactionDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.TotalPointsDto;
import at.control.shiftcontrol.shift.service.rewardpoints.RewardPointsLedgerService;

@Service
@RequiredArgsConstructor
public class RewardPointsLedgerServiceImpl implements RewardPointsLedgerService {
    private final RewardPointsTransactionDao dao;
    private final VolunteerDao volunteerDao;
    private final EventDao eventDao;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public BookingResultDto bookEarn(RewardPointsTransactionDto dto) {
        return insert(
            dto.getUserId(),
            dto.getEventId(),
            dto.getShiftPlanId(),
            dto.getPositionSlotId(),
            dto.getPointsSnapshot(),
            RewardPointTransactionType.EARN,
            dto.getSourceKey(),
            dto.getMetadata()
        );
    }

    @Override
    @Transactional
    public BookingResultDto bookReversal(RewardPointsTransactionDto dto) {
        // reversal is always negative of the snapshot
        int points = -dto.getPointsSnapshot();

        return insert(
            dto.getUserId(),
            dto.getEventId(),
            dto.getShiftPlanId(),
            dto.getPositionSlotId(),
            points,
            RewardPointTransactionType.REVERSAL,
            dto.getSourceKey(),
            dto.getMetadata()
        );
    }

    @Override
    @Transactional
    public BookingResultDto bookManualAdjust(RewardPointsTransactionDto dto) {
        return insert(
            dto.getUserId(),
            dto.getEventId(),
            dto.getShiftPlanId(),
            dto.getPositionSlotId(),
            dto.getPointsSnapshot(),
            RewardPointTransactionType.MANUAL_ADJUST,
            dto.getSourceKey(),
            dto.getMetadata()
        );
    }

    private BookingResultDto insert(
        UUID userId,
        long eventId,
        Long shiftPlanId,
        Long positionSlotId,
        int points,
        RewardPointTransactionType type,
        String sourceKey,
        Map<String, Object> metadata
    ) {
        validateInputs(userId, eventId, points, sourceKey, type);

        RewardPointsTransaction tx = RewardPointsTransaction.builder()
            .volunteerId(userId.toString())
            .eventId(eventId)
            .shiftPlanId(shiftPlanId) // optional
            .positionSlotId(positionSlotId) // optional
            .points(points)
            .type(type)
            .sourceKey(sourceKey)
            .metadata(metadata)
            .createdAt(Instant.now())
            .build();

        try {
            RewardPointsTransaction saved = dao.save(tx);

            publisher.publishEvent(RewardPointTransactionEvent.transactionCreated(saved));
            return new BookingResultDto(true, saved);
        } catch (DataIntegrityViolationException _) {
            // publish failed event with uncommitted transaction data
            publisher.publishEvent(RewardPointTransactionEvent.transactionFailed(tx));
            return new BookingResultDto(false, null);
        }
    }

    private void validateInputs(UUID userId, long eventId, int points, String sourceKey, RewardPointTransactionType type) {
        volunteerDao.getById(userId);
        eventDao.getById(eventId);
        if (sourceKey == null || sourceKey.isBlank()) {
            throw new IllegalArgumentException("sourceKey must not be blank");
        }
        // allow negative points only for REVERSAL and MANUAL_ADJUST
        if (type == RewardPointTransactionType.EARN && points < 0) {
            throw new IllegalArgumentException("EARN points must be >= 0");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TotalPointsDto getTotalPoints(UUID userId) {
        var totalPoints = dao.sumPointsByVolunteer(userId.toString());

        return TotalPointsDto.builder()
            .totalPoints((int) totalPoints)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EventPointsDto getPointsForEvent(UUID userId, long eventId) {
        var evenPoints = dao.sumPointsByVolunteerAndEvent(userId.toString(), eventId);

        return EventPointsDto.builder()
            .eventId(String.valueOf(eventId))
            .points((int) evenPoints)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<EventPointsDto> getPointsGroupedByEvent(UUID userId) {
        return dao.sumPointsForUserGroupedByEvent(userId.toString());
    }
}

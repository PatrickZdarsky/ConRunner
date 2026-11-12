package at.conrunner.shift.service.rewardpoints.impl;

import java.util.UUID;
import java.time.Instant;
import java.util.Collection;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import at.conrunner.shift.entity.RewardPointsTransaction;
import at.conrunner.shift.lib.event.events.RewardPointTransactionEvent;
import at.conrunner.platform.exception.IllegalArgumentException;
import at.conrunner.shift.lib.type.RewardPointTransactionType;
import at.conrunner.shift.dao.EventDao;
import at.conrunner.shift.dao.RewardPointsTransactionDao;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.dto.rewardpoints.BookingResultDto;
import at.conrunner.shift.dto.rewardpoints.EventPointsDto;
import at.conrunner.shift.dto.rewardpoints.RewardPointsTransactionDto;
import at.conrunner.shift.dto.rewardpoints.TotalPointsDto;
import at.conrunner.shift.service.rewardpoints.RewardPointsLedgerService;

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
        return insert(dto, RewardPointTransactionType.EARN, dto.getPointsSnapshot());
    }

    @Override
    @Transactional
    public BookingResultDto bookReversal(RewardPointsTransactionDto dto) {
        // reversal is always negative of the snapshot
        int points = -dto.getPointsSnapshot();

        return insert(dto, RewardPointTransactionType.REVERSAL, points);
    }

    @Override
    @Transactional
    public BookingResultDto bookManualAdjust(RewardPointsTransactionDto dto) {
        return insert(dto, RewardPointTransactionType.MANUAL_ADJUST, dto.getPointsSnapshot());
    }

    private BookingResultDto insert(RewardPointsTransactionDto dto, RewardPointTransactionType type, int points) {
        validateInputs(dto.getUserId(), dto.getEventId(), points, dto.getSourceKey(), type);

        RewardPointsTransaction tx = RewardPointsTransaction.builder()
            .volunteerId(dto.getUserId().toString())
            .eventId(dto.getEventId())
            .shiftPlanId(dto.getShiftPlanId())
            .positionSlotId(dto.getPositionSlotId())
            .points(points)
            .type(type)
            .sourceKey(dto.getSourceKey())
            .metadata(dto.getMetadata())
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

package at.control.shiftcontrol.shift.service.rewardpoints;

import java.util.UUID;
import java.util.Collection;

import at.control.shiftcontrol.shift.dto.rewardpoints.BookingResultDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.EventPointsDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.RewardPointsTransactionDto;
import at.control.shiftcontrol.shift.dto.rewardpoints.TotalPointsDto;

public interface RewardPointsLedgerService {
    /* ========= WRITE (append-only) ========= */

    BookingResultDto bookEarn(RewardPointsTransactionDto dto);

    BookingResultDto bookReversal(RewardPointsTransactionDto dto);

    BookingResultDto bookManualAdjust(RewardPointsTransactionDto dto);

    /* ========= READ (aggregation) ========= */

    TotalPointsDto getTotalPoints(UUID userId);

    EventPointsDto getPointsForEvent(UUID userId, long eventId);

    Collection<EventPointsDto> getPointsGroupedByEvent(UUID userId);
}

package at.control.shiftcontrol.shift.dao;

import java.util.Collection;

import at.control.shiftcontrol.shift.entity.RewardPointsTransaction;
import at.control.shiftcontrol.shift.dto.rewardpoints.EventPointsDto;

public interface RewardPointsTransactionDao extends BasicDao<RewardPointsTransaction, Long> {

    long sumPointsByVolunteer(String volunteerId);

    long sumPointsByVolunteerAndEvent(String volunteerId, long eventId);

    long sumPointsByVolunteerAndShiftPlan(String volunteerId, long shiftPlanId);

    Collection<EventPointsDto> sumPointsForUserGroupedByEvent(String volunteerId);

    Collection<RewardPointsTransaction> findAllByVolunteerIdOrderByCreatedAtAsc(String volunteerId);
}

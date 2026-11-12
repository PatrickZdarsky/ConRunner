package at.conrunner.shift.service.impl;

import java.util.UUID;
import java.util.ArrayList;

import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.platform.exception.ForbiddenException;
import at.conrunner.platform.exception.NotFoundException;
import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.dao.AssignmentDao;
import at.conrunner.shift.dao.AssignmentSwitchRequestDao;
import at.conrunner.shift.dao.EventDao;
import at.conrunner.shift.dao.RewardPointsTransactionDao;
import at.conrunner.shift.dao.ShiftDao;
import at.conrunner.shift.dao.ShiftPlanDao;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.dto.event.EventsDashboardOverviewDto;
import at.conrunner.shift.dto.shiftplan.ShiftPlanDashboardOverviewDto;
import at.conrunner.shift.mapper.AssignmentAssemblingMapper;
import at.conrunner.shift.mapper.EventMapper;
import at.conrunner.shift.mapper.ShiftAssemblingMapper;
import at.conrunner.shift.mapper.ShiftPlanMapper;
import at.conrunner.shift.mapper.TradeMapper;
import at.conrunner.shift.service.DashboardService;
import at.conrunner.shift.service.EligibilityService;
import at.conrunner.shift.service.StatisticService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {
    private final StatisticService statisticService;
    private final EligibilityService eligibilityService;
    private final ShiftPlanDao shiftPlanDao;
    private final ShiftDao shiftDao;
    private final EventDao eventDao;
    private final AssignmentDao assignmentDao;
    private final VolunteerDao volunteerDao;
    private final AssignmentSwitchRequestDao assignmentSwitchRequestDao;
    private final RewardPointsTransactionDao rewardPointsTransactionDao;
    private final CurrentShiftActor currentActor;
    private final ShiftAssemblingMapper shiftMapper;
    private final TradeMapper tradeMapper;
    private final AssignmentAssemblingMapper assignmentAssemblingMapper;

    @Override
    public @NonNull EventsDashboardOverviewDto getDashboardOverviewsOfAllShiftPlans(@NonNull String eventId, @NonNull UUID userId) {
        var userShiftPlans = shiftPlanDao.findAllUserRelatedShiftPlansInEvent(userId, eventId);

        var shiftPlanDashboards = userShiftPlans.stream().map(shiftPlan -> {
            try {
                return getDashboardOverviewOfShiftPlan(shiftPlan.getId());
            } catch (NotFoundException | ForbiddenException e) {
                // This should not happen as we already fetched user related shift plans
                throw new RuntimeException(e);
            }
        }).toList();

        return EventsDashboardOverviewDto.builder()
            .shiftPlanDashboardOverviewDtos(shiftPlanDashboards)
            .ownStatisticsDto(statisticService.getOwnStatisticsOfShiftPlans(userShiftPlans.stream().toList(), userId))
            .build();
    }

    private ShiftPlanDashboardOverviewDto getDashboardOverviewOfShiftPlan(long shiftPlanId) {
        var userId = validateShiftPlanAccessAndGetUserId(shiftPlanId);
        var shiftPlan = getShiftPlanOrThrow(shiftPlanId);
        var event = eventDao.getById(shiftPlan.getEvent().getId());
        var userShifts = shiftDao.searchUserRelatedShiftsInShiftPlan(shiftPlanId, userId);
        var auctions = assignmentDao.findAuctionsByShiftPlanId(shiftPlanId);
        var volunteer = volunteerDao.getById(userId);

        // only show auctions where user is eligible to participate or own auctions
        var eligibleAuctions = auctions.stream().filter(
            assignment -> eligibilityService.isEligibleAndNotSignedUp(
                assignment.getPositionSlot(),
                volunteer
            )).toList();
        var ownAuctions = auctions.stream().filter(
            assignment -> assignment.getAssignedVolunteer() != null &&
                assignment.getAssignedVolunteer().getId().equals(userId)
        ).toList();

        // add both eligible and own auctions, avoiding duplicates
        var combinedAuctions = new ArrayList<Assignment>();
        combinedAuctions.addAll(eligibleAuctions);
        combinedAuctions.addAll(ownAuctions.stream()
            .filter(ownAuction -> !eligibleAuctions.contains(ownAuction))
            .toList());

        return ShiftPlanDashboardOverviewDto.builder()
            .shiftPlan(ShiftPlanMapper.toShiftPlanDto(shiftPlan))
            .eventOverview(EventMapper.toEventDto(event))
            .ownShiftPlanStatistics(statisticService.getOwnStatisticsOfShifts(userShifts)) // directly pass user shifts here to avoid redundant filtering
            .overallShiftPlanStatistics(statisticService.getOverallShiftPlanStatistics(shiftPlan))
            .rewardPoints((int) rewardPointsTransactionDao.sumPointsByVolunteerAndShiftPlan(userId.toString(), shiftPlanId))
            .shifts(shiftMapper.assemble(userShifts))
            .trades(tradeMapper.toTradeInfoDto(assignmentSwitchRequestDao.findTradesForShiftPlanAndUser(shiftPlanId, userId)))
            .auctions(assignmentAssemblingMapper.toContextDto(combinedAuctions))
            .build();
    }

    private UUID validateShiftPlanAccessAndGetUserId(long shiftPlanId) {
        var actor = currentActor.get();
        if (!(actor.isVolunteerInPlan(shiftPlanId) || actor.isPlannerInPlan(shiftPlanId))) {
            log.error("User has no access to shift plan with id: {}", shiftPlanId);
            throw new ForbiddenException("User has no access to shift plan.");
        }

        return actor.requireUserId();
    }

    private ShiftPlan getShiftPlanOrThrow(long shiftPlanId) {
        return shiftPlanDao.getById(shiftPlanId);
    }
}

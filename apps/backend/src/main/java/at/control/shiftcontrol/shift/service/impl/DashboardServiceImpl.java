package at.control.shiftcontrol.shift.service.impl;

import java.util.ArrayList;

import at.shiftcontrol.lib.entity.Assignment;
import at.shiftcontrol.lib.entity.ShiftPlan;
import at.shiftcontrol.lib.exception.ForbiddenException;
import at.shiftcontrol.lib.exception.NotFoundException;
import at.shiftcontrol.shiftservice.auth.ApplicationUserProvider;
import at.shiftcontrol.shiftservice.dao.AssignmentDao;
import at.shiftcontrol.shiftservice.dao.AssignmentSwitchRequestDao;
import at.shiftcontrol.shiftservice.dao.EventDao;
import at.shiftcontrol.shiftservice.dao.RewardPointsTransactionDao;
import at.shiftcontrol.shiftservice.dao.ShiftDao;
import at.shiftcontrol.shiftservice.dao.ShiftPlanDao;
import at.shiftcontrol.shiftservice.dao.userprofile.VolunteerDao;
import at.shiftcontrol.shiftservice.dto.event.EventsDashboardOverviewDto;
import at.shiftcontrol.shiftservice.dto.shiftplan.ShiftPlanDashboardOverviewDto;
import at.shiftcontrol.shiftservice.mapper.AssignmentAssemblingMapper;
import at.shiftcontrol.shiftservice.mapper.EventMapper;
import at.shiftcontrol.shiftservice.mapper.ShiftAssemblingMapper;
import at.shiftcontrol.shiftservice.mapper.ShiftPlanMapper;
import at.shiftcontrol.shiftservice.mapper.TradeMapper;
import at.shiftcontrol.shiftservice.service.DashboardService;
import at.shiftcontrol.shiftservice.service.EligibilityService;
import at.shiftcontrol.shiftservice.service.StatisticService;
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
    private final ApplicationUserProvider userProvider;
    private final ShiftAssemblingMapper shiftMapper;
    private final TradeMapper tradeMapper;
    private final AssignmentAssemblingMapper assignmentAssemblingMapper;

    @Override
    public @NonNull EventsDashboardOverviewDto getDashboardOverviewsOfAllShiftPlans(@NonNull String eventId, @NonNull String userId) {
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
            .rewardPoints((int) rewardPointsTransactionDao.sumPointsByVolunteerAndShiftPlan(userId, shiftPlanId))
            .shifts(shiftMapper.assemble(userShifts))
            .trades(tradeMapper.toTradeInfoDto(assignmentSwitchRequestDao.findTradesForShiftPlanAndUser(shiftPlanId, userId)))
            .auctions(assignmentAssemblingMapper.toContextDto(combinedAuctions))
            .build();
    }

    private String validateShiftPlanAccessAndGetUserId(long shiftPlanId) {
        var currentUser = userProvider.getCurrentUser();
        if (!(currentUser.isVolunteerInPlan(shiftPlanId) || currentUser.isPlannerInPlan(shiftPlanId))) {
            log.error("User has no access to shift plan with id: {}", shiftPlanId);
            throw new ForbiddenException("User has no access to shift plan.");
        }

        return currentUser.getUserId();
    }

    private ShiftPlan getShiftPlanOrThrow(long shiftPlanId) {
        return shiftPlanDao.getById(shiftPlanId);
    }
}

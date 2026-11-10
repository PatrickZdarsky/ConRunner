package at.control.shiftcontrol.shift.service.impl;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.PositionSlot;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;
import at.control.shiftcontrol.shift.lib.util.TimeUtil;
import at.control.shiftcontrol.shift.dao.ShiftDao;
import at.control.shiftcontrol.shift.dto.OverallStatisticsDto;
import at.control.shiftcontrol.shift.dto.OwnStatisticsDto;
import at.control.shiftcontrol.shift.dto.event.schedule.ScheduleStatisticsDto;
import at.control.shiftcontrol.shift.service.EligibilityService;
import at.control.shiftcontrol.shift.service.StatisticService;

@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {
    private final ShiftDao shiftDao;
    private final EligibilityService eligibilityService;

    @Override
    public @NonNull OwnStatisticsDto getOwnStatisticsOfShifts(@NonNull List<Shift> userShifts) {
        var busyDays = calculateBusyDaysInShifts(userShifts);
        var totalShifts = calculateTotalShiftCountInShifts(userShifts);
        var totalHours = calculateTotalHoursInShifts(userShifts);

        return OwnStatisticsDto.builder()
            .busyDays(busyDays)
            .totalShifts(totalShifts)
            .totalHours(totalHours)
            .build();
    }


    @Override
    public @NonNull OverallStatisticsDto getOverallShiftPlanStatistics(@NonNull ShiftPlan shiftPlan) {
        var totalShifts = shiftPlan.getShifts().size();
        var totalHours = calculateTotalHoursInShifts(shiftPlan.getShifts().stream().toList());
        var volunteerCount = calculateVolunteerCountInShiftPlan(shiftPlan);

        return OverallStatisticsDto.builder()
            .totalHours(totalHours)
            .totalShifts(totalShifts)
            .volunteerCount(volunteerCount)
            .build();
    }

    @Override
    public @NonNull OverallStatisticsDto getOverallEventStatistics(@NonNull Event event) {
        var totalHours = event.getShiftPlans().stream()
            .mapToDouble(shiftPlan ->
                calculateTotalHoursInShifts(shiftPlan.getShifts().stream().toList()))
            .sum();
        var totalShifts = event.getShiftPlans().stream()
            .mapToInt(shiftPlan ->
                calculateTotalShiftCountInShifts(shiftPlan.getShifts().stream().toList()))
            .sum();
        var volunteerCount = calculateVolunteerCountInEvent(event);

        return OverallStatisticsDto.builder()
            .totalHours(totalHours)
            .totalShifts(totalShifts)
            .volunteerCount(volunteerCount)
            .build();
    }

    @Override
    public @NonNull OwnStatisticsDto getOwnStatisticsOfShiftPlans(@NonNull List<ShiftPlan> shiftPlans, @NonNull String userId) {
        var userShiftsSet = new HashSet<Shift>();
        for (var shiftPlan : shiftPlans) {
            var relevantShiftsOfPlan = shiftDao.searchUserRelatedShiftsInShiftPlan(shiftPlan.getId(), userId);
            userShiftsSet.addAll(relevantShiftsOfPlan);
        }

        return getOwnStatisticsOfShifts(userShiftsSet.stream().toList());
    }

    @Override
    public @NonNull ScheduleStatisticsDto getShiftPlanScheduleStatistics(@NonNull List<Shift> shifts, @NonNull Volunteer volunteer) {
        double totalHours = shifts.stream()
            .mapToDouble(s -> TimeUtil.calculateDurationInMinutes(s.getStartTime(), s.getEndTime()))
            .sum() / 60.0;

        var slots = shifts.stream()
            .flatMap(s -> s.getSlots().stream()).toList();

        var totalRequiredSlotCount = slots.stream()
            .mapToInt(PositionSlot::getDesiredVolunteerCount)
            .sum();

        var assignedSlotCount = slots.stream()
            .mapToInt(s -> (int) s.getAssignments().stream()
                .filter(a -> a.getAssignedVolunteer() != null
                    && a.getStatus() != AssignmentStatus.REQUEST_FOR_ASSIGNMENT)
                .count())
            .sum();

        int unassignedCount = totalRequiredSlotCount - assignedSlotCount;

        // calculate shift count for signup (count of distinct shifts with at least one unassigned slot where user is eligible to sign up)
        var shiftWithUnassignedSlots = new HashSet<Shift>();
        for (var shift : shifts) {
            for (var slot : shift.getSlots()) {
                var assignedCount = slot.getAssignments().stream()
                    .filter(a -> a.getAssignedVolunteer() != null
                        && a.getStatus() != AssignmentStatus.REQUEST_FOR_ASSIGNMENT)
                    .count();
                if (assignedCount < slot.getDesiredVolunteerCount()
                    && eligibilityService.isEligibleAndNotSignedUp(slot, volunteer)) {
                    var conflictingAssignments = eligibilityService.getConflictingAssignments(
                        volunteer.getId().toString(),
                        slot.getShift().getStartTime(),
                        slot.getShift().getEndTime());

                    if (conflictingAssignments.isEmpty()) {
                        shiftWithUnassignedSlots.add(shift);
                        break;
                    }
                }
            }
        }
        int shiftCountForSignup = shiftWithUnassignedSlots.size();

        return ScheduleStatisticsDto.builder()
            .totalShifts(shifts.size())
            .totalHours(totalHours)
            .unassignedCount(unassignedCount)
            .shiftCountForSignup(shiftCountForSignup)
            .build();
    }

    private int calculateBusyDaysInShifts(List<Shift> userShifts) {
        var busyDaysSet = new HashSet<LocalDate>();
        for (var shift : userShifts) {
            var shiftDate = TimeUtil.convertToUtcLocalDate(shift.getStartTime());
            busyDaysSet.add(shiftDate);
        }
        return busyDaysSet.size();
    }

    private int calculateTotalShiftCountInShifts(List<Shift> userShifts) {
        return userShifts.size();
    }

    private double calculateTotalHoursInShifts(List<Shift> userShifts) {
        double totalMinutes = 0;
        for (var shift : userShifts) {
            var duration = TimeUtil.calculateDurationInMinutes(shift.getStartTime(), shift.getEndTime());
            totalMinutes += duration;
        }
        return totalMinutes / 60;
    }

    private int calculateVolunteerCountInShiftPlan(ShiftPlan shiftPlan) {
//         var volunteerSet = new HashSet<String>();
//         for (var shift : shiftPlan.getShifts()) {
//             for (var slot : shift.getSlots()) {
//                 for (var assignment : slot.getAssignments()) {
//                     if (assignment.getAssignedVolunteer() != null) {
//                         volunteerSet.add(assignment.getAssignedVolunteer().getId());
//                     }
//                 }
//             }
//         }
        return shiftPlan.getPlanVolunteers().size();
    }

    private int calculateVolunteerCountInEvent(Event event) {
        var volunteerSet = new HashSet<String>();
        for (var shiftPlan : event.getShiftPlans()) {
            volunteerSet.addAll(shiftPlan.getPlanVolunteers().stream().map(v -> v.getId().toString()).toList());
        }
        return volunteerSet.size();
    }
}

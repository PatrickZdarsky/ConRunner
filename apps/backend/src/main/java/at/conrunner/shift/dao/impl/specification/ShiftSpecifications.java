package at.conrunner.shift.dao.impl.specification;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.time.ZoneOffset;

import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.lib.util.TimeUtil;
import at.conrunner.shift.dto.event.schedule.EventScheduleDaySearchDto;
import at.conrunner.shift.dto.event.schedule.EventScheduleFilterDto;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class ShiftSpecifications {
    private ShiftSpecifications() {
    }

    public static Specification<Shift> inShiftPlan(long shiftPlanId) {
        // Shift -> shiftPlan -> id
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("shiftPlan").get("id"), shiftPlanId);
    }

    public static Specification<Shift> inEvent(long eventId) {
        // Shift -> shiftPlan -> event -> id
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("shiftPlan").get("event").get("id"), eventId);
    }

    public static Specification<Shift> assignedToUser(UUID userId) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true); // important because of joins to collections

            // user-related restriction: Shift -> slots -> assignments -> assignedVolunteer.id
            var slots = root.join("slots", JoinType.INNER);
            var assignments = slots.join("assignments", JoinType.INNER);
            var volunteer = assignments.join("assignedVolunteer", JoinType.INNER);

            return criteriaBuilder.equal(volunteer.get("id"), userId);
        };
    }

    public static Specification<Shift> matchesSearchDto(EventScheduleFilterDto filterDto) {
        return (root, query, criteriaBuilder) -> {
            if (filterDto == null) {
                return criteriaBuilder.conjunction();
            }

            var predicates = criteriaBuilder.conjunction();

            // date filter (LocalDate -> Instant range)
            if (filterDto instanceof EventScheduleDaySearchDto daySearchDto && daySearchDto.getDate() != null) {
                Instant dayStart = TimeUtil.convertToStartOfUtcDayInstant(daySearchDto.getDate());
                Instant nextDayStart = daySearchDto.getDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

                predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.lessThan(root.get("startTime"), nextDayStart),
                    criteriaBuilder.greaterThan(root.get("endTime"), dayStart)
                );
            }

            if (!CollectionUtils.isEmpty(filterDto.getShiftPlanIds())) {
                // shiftPlan IDs
                predicates = criteriaBuilder.and(predicates,
                    root.get("shiftPlan").get("id").in(
                        nonBlankIds(filterDto.getShiftPlanIds())
                    )
                );
            }

            // shift name contains (case-insensitive)
            if (filterDto.getShiftName() != null && !filterDto.getShiftName().isBlank()) {
                String pattern = "%" + filterDto.getShiftName().toLowerCase().trim() + "%";
                predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        pattern
                    ));
            }

            // roleNames: Shift -> slots --> role --> id
            if (!CollectionUtils.isEmpty(filterDto.getRoleIds())) {
                var roleJoin = root
                    .join("slots", JoinType.INNER)
                    .join("role", JoinType.INNER);


                // filter only non-null, non-blank role IDs
                predicates = criteriaBuilder.and(predicates,
                    roleJoin.get("id").in(
                        nonBlankIds(filterDto.getRoleIds())
                    )
                );
            }

            if (!CollectionUtils.isEmpty(filterDto.getLocationIds())) {
                var directLocation = root.join("location", JoinType.LEFT);
                var activity = root.join("relatedActivity", JoinType.LEFT);
                var activityLocation = activity.join("location", JoinType.LEFT);
                var locationIds = nonBlankIds(filterDto.getLocationIds());
                predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.or(
                        criteriaBuilder.and(criteriaBuilder.isNull(activity.get("id")), directLocation.get("id").in(locationIds)),
                        activityLocation.get("id").in(locationIds)
                    )
                );
            }

            return predicates;
        };
    }

    private static List<String> nonBlankIds(Collection<String> ids) {
        return ids.stream().filter(StringUtils::hasText).toList();
    }
}

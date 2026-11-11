package at.control.shiftcontrol.shift.dao.impl.specification;

import java.util.UUID;
import java.time.Instant;
import java.time.ZoneOffset;

import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.lib.util.TimeUtil;
import at.control.shiftcontrol.shift.dto.event.schedule.EventScheduleDaySearchDto;
import at.control.shiftcontrol.shift.dto.event.schedule.EventScheduleFilterDto;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

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

            if (filterDto.getShiftPlanIds() != null && !filterDto.getShiftPlanIds().isEmpty()) {
                // shiftPlan IDs
                predicates = criteriaBuilder.and(predicates,
                    root.get("shiftPlan").get("id").in(
                        filterDto.getShiftPlanIds().stream()
                            .filter(s -> s != null && !s.isBlank())
                            .toList()
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
            if (filterDto.getRoleIds() != null && !filterDto.getRoleIds().isEmpty()) {
                var roleJoin = root
                    .join("slots", JoinType.INNER)
                    .join("role", JoinType.INNER);


                // filter only non-null, non-blank role IDs
                predicates = criteriaBuilder.and(predicates,
                    roleJoin.get("id").in(
                        filterDto.getRoleIds().stream()
                            .filter(s -> s != null && !s.isBlank())
                            .toList()
                    )
                );
            }

            // locations: Shift -> locations --> id
            if (filterDto.getLocationIds() != null && !filterDto.getLocationIds().isEmpty()) {
                var locationJoin = root.join("location", JoinType.INNER);

                // filter only non-null, non-blank location IDs
                predicates = criteriaBuilder.and(predicates,
                    locationJoin.get("id").in(
                        filterDto.getLocationIds().stream()
                            .filter(s -> s != null && !s.isBlank())
                            .toList()
                    )
                );
            }

            return predicates;
        };
    }
}


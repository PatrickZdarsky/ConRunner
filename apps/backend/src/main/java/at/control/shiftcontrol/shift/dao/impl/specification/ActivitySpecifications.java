package at.control.shiftcontrol.shift.dao.impl.specification;

import java.time.Instant;
import java.time.ZoneOffset;

import at.control.shiftcontrol.shift.entity.Activity;
import at.control.shiftcontrol.shift.lib.util.TimeUtil;
import at.control.shiftcontrol.shift.dto.event.schedule.ActivityScheduleDaySearchDto;
import org.springframework.data.jpa.domain.Specification;

public final class ActivitySpecifications {
    private ActivitySpecifications() {
    }

    public static Specification<Activity> matchesSearchDto(ActivityScheduleDaySearchDto daySearchDto) {
        return (root, query, criteriaBuilder) -> {
            if (daySearchDto == null) {
                return criteriaBuilder.conjunction();
            }

            var predicates = criteriaBuilder.conjunction();

            // date filter (LocalDate -> Instant range)
            if (daySearchDto.getDate() != null) {
                Instant dayStart = TimeUtil.convertToStartOfUtcDayInstant(daySearchDto.getDate());
                Instant nextDayStart = daySearchDto.getDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

                predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.lessThan(root.get("startTime"), nextDayStart),
                    criteriaBuilder.greaterThan(root.get("endTime"), dayStart)
                );
            }

            return predicates;
        };
    }
}


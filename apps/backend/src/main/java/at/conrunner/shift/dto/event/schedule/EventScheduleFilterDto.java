package at.conrunner.shift.dto.event.schedule;

import java.util.Collection;

import at.conrunner.shift.lib.type.ShiftRelevance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class EventScheduleFilterDto {
    private Collection<String> shiftPlanIds;
    private String shiftName;
    private Collection<ShiftRelevance> shiftRelevances;
    private Collection<String> roleIds;
    private Collection<String> locationIds;
}

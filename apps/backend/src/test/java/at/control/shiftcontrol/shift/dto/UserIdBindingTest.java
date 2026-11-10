package at.control.shiftcontrol.shift.dto;

import java.util.UUID;

import at.control.shiftcontrol.shift.dto.assignment.AssignmentAssignDto;
import at.control.shiftcontrol.shift.dto.trade.TradeCreateDto;
import at.control.shiftcontrol.shift.dto.user.UserPlanBulkDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdBindingTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void userIdsInRequestBodiesBindAsUuids() {
        var id = UUID.randomUUID();
        var assignment = mapper.readValue("{\"positionSlotId\":\"1\",\"volunteerIds\":[\"" + id + "\"]}", AssignmentAssignDto.class);
        var trade = mapper.readValue("{\"offeredPositionSlotId\":\"1\",\"requestedPositionSlotId\":\"2\",\"requestedVolunteerIds\":[\"" + id + "\"]}", TradeCreateDto.class);
        var bulk = mapper.readValue("{\"roles\":[],\"volunteers\":[\"" + id + "\"]}", UserPlanBulkDto.class);

        assertThat(assignment.getVolunteerIds()).containsExactly(id);
        assertThat(trade.getRequestedVolunteerIds()).containsExactly(id);
        assertThat(bulk.getVolunteers()).containsExactly(id);
    }

    @Test
    void malformedUserIdIsRejectedDuringRequestBinding() {
        assertThatThrownBy(() -> mapper.readValue("{\"volunteerIds\":[\"invalid\"]}", AssignmentAssignDto.class))
            .isInstanceOf(RuntimeException.class);
    }
}

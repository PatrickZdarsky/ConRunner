package at.control.shiftcontrol.shift.endpoint.leaderboard;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.control.shiftcontrol.shift.lib.exception.ForbiddenException;
import at.control.shiftcontrol.shift.lib.exception.NotFoundException;
import at.control.shiftcontrol.shift.lib.util.ConvertUtil;
import at.control.shiftcontrol.shift.auth.ApplicationUserProvider;
import at.control.shiftcontrol.shift.dto.leaderboard.LeaderBoardDto;
import at.control.shiftcontrol.shift.service.LeaderboardService;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/events/{eventId}/leaderboard", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LeaderboardEndpoint {
    private final LeaderboardService leaderboardService;
    private final ApplicationUserProvider userProvider;

    @GetMapping()
    @Operation(
        operationId = "getLeaderboardForEvent",
        description = "Get the leaderboard for a specific event"
    )
    public LeaderBoardDto getLeaderboard(@PathVariable String eventId) throws NotFoundException, ForbiddenException {
        var currentUser = userProvider.getCurrentUser();
        return leaderboardService.getLeaderBoard(ConvertUtil.idToLong(eventId), currentUser.getUserId());
    }
}

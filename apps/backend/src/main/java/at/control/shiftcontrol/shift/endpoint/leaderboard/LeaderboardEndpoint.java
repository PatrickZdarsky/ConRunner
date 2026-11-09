package at.control.shiftcontrol.shift.endpoint.leaderboard;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.shiftcontrol.lib.exception.ForbiddenException;
import at.shiftcontrol.lib.exception.NotFoundException;
import at.shiftcontrol.lib.util.ConvertUtil;
import at.shiftcontrol.shiftservice.auth.ApplicationUserProvider;
import at.shiftcontrol.shiftservice.dto.leaderboard.LeaderBoardDto;
import at.shiftcontrol.shiftservice.service.LeaderboardService;

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

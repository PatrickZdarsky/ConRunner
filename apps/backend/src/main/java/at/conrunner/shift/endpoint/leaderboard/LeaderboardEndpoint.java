package at.conrunner.shift.endpoint.leaderboard;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.conrunner.platform.exception.ForbiddenException;
import at.conrunner.platform.exception.NotFoundException;
import at.conrunner.shift.lib.util.ConvertUtil;
import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.dto.leaderboard.LeaderBoardDto;
import at.conrunner.shift.service.LeaderboardService;

@Slf4j
@RestController
@RequestMapping(value = "api/v1/events/{eventId}/leaderboard", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LeaderboardEndpoint {
    private final LeaderboardService leaderboardService;
    private final CurrentShiftActor currentActor;

    @GetMapping()
    @Operation(
        operationId = "getLeaderboardForEvent",
        description = "Get the leaderboard for a specific event"
    )
    public LeaderBoardDto getLeaderboard(@PathVariable String eventId) throws NotFoundException, ForbiddenException {
        var actor = currentActor.get();
        return leaderboardService.getLeaderBoard(ConvertUtil.idToLong(eventId), actor.requireUserId());
    }
}

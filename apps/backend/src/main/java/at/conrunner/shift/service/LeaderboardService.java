package at.conrunner.shift.service;

import java.util.UUID;
import lombok.NonNull;

import at.conrunner.platform.exception.ForbiddenException;
import at.conrunner.platform.exception.NotFoundException;
import at.conrunner.shift.dto.leaderboard.LeaderBoardDto;

public interface LeaderboardService {
    @NonNull LeaderBoardDto getLeaderBoard(long eventId, @NonNull UUID currentUserId) throws NotFoundException, ForbiddenException;
}

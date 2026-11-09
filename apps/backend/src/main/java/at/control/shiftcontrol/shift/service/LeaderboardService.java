package at.control.shiftcontrol.shift.service;

import lombok.NonNull;

import at.shiftcontrol.lib.exception.ForbiddenException;
import at.shiftcontrol.lib.exception.NotFoundException;
import at.shiftcontrol.shiftservice.dto.leaderboard.LeaderBoardDto;

public interface LeaderboardService {
    @NonNull LeaderBoardDto getLeaderBoard(long eventId, @NonNull String currentUserId) throws NotFoundException, ForbiddenException;
}

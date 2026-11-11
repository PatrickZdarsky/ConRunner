package at.control.shiftcontrol.shift.service;

import java.util.UUID;
import lombok.NonNull;

import at.control.shiftcontrol.platform.exception.ForbiddenException;
import at.control.shiftcontrol.platform.exception.NotFoundException;
import at.control.shiftcontrol.shift.dto.leaderboard.LeaderBoardDto;

public interface LeaderboardService {
    @NonNull LeaderBoardDto getLeaderBoard(long eventId, @NonNull UUID currentUserId) throws NotFoundException, ForbiddenException;
}

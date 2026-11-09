package at.control.shiftcontrol.shift.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.shiftcontrol.lib.entity.Assignment;
import at.shiftcontrol.lib.entity.Shift;
import at.shiftcontrol.lib.exception.ForbiddenException;
import at.shiftcontrol.lib.exception.NotFoundException;
import at.shiftcontrol.shiftservice.dao.EventDao;
import at.shiftcontrol.shiftservice.dto.leaderboard.LeaderBoardDto;
import at.shiftcontrol.shiftservice.dto.leaderboard.RankDto;
import at.shiftcontrol.shiftservice.service.LeaderboardService;
import at.shiftcontrol.shiftservice.userdirectory.UserInfo;
import at.shiftcontrol.shiftservice.userdirectory.UserLookupService;
import at.shiftcontrol.shiftservice.util.SecurityHelper;

@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {
    private static final int LEADERBOARD_LIMIT = 10;

    private final EventDao eventDao;
    private final SecurityHelper securityHelper;
    private final UserLookupService userDirectoryService;

    @Override
    public @NonNull LeaderBoardDto getLeaderBoard(long eventId, @NonNull String currentUserId) throws NotFoundException, ForbiddenException {
        var event = eventDao.findById(eventId).orElseThrow(NotFoundException::new);
        securityHelper.assertUserIsAllowedToAccessEvent(event);

        List<Assignment> allAssignments = ensureList(event.getShiftPlans()).stream()
            .flatMap(plan -> ensureList(plan.getShifts()).stream())
            .flatMap(shift -> ensureList(shift.getSlots()).stream())
            .flatMap(slot -> ensureList(slot.getAssignments()).stream())
            .toList();

        Map<String, Long> minutesByVolunteer = new HashMap<>();

        for (Assignment a : allAssignments) {
            Shift shift = a.getPositionSlot().getShift();

            long minutes = Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes();
            String volunteerId = a.getAssignedVolunteer().getId();
            minutesByVolunteer.merge(volunteerId, minutes, Long::sum);
        }

        List<String> topVolunteerIds = minutesByVolunteer.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .map(Map.Entry::getKey)
            .toList();

        var userIdsToResolve = new LinkedHashSet<>(topVolunteerIds);
        userIdsToResolve.add(currentUserId);
        Map<String, UserInfo> usersById = userDirectoryService.getUserByIds(userIdsToResolve).stream()
            .collect(java.util.stream.Collectors.toMap(UserInfo::id, user -> user));

        RankDto ownRank = null;
        if (minutesByVolunteer.containsKey(currentUserId)) {
            var user = usersById.get(currentUserId);
            ownRank = RankDto.builder()
                .rank(topVolunteerIds.indexOf(currentUserId) + 1)
                .hours(minutesByVolunteer.get(currentUserId) / 60)
                .firstName(user.firstName())
                .lastName(user.lastName())
                .build();
        }

        List<RankDto> ranks = new ArrayList<>(topVolunteerIds.size());
        Long prevMinutes = null;
        int position = 0;
        int displayRank = 0;

        for (String volunteerId : topVolunteerIds) {
            position++;
            long minutes = minutesByVolunteer.get(volunteerId);

            if (prevMinutes == null || minutes != prevMinutes) {
                displayRank = position; // jump when stats change
                prevMinutes = minutes;
            }

            var user = usersById.get(volunteerId);
            long hours = minutes / 60;

            var dto = RankDto.builder()
                .rank(displayRank)
                .hours(hours)
                .firstName(user.firstName())
                .lastName(user.lastName())
                .build();

            if (volunteerId.equals(currentUserId)) {
                ownRank = dto;
            }

            // leaderboard list: only add top N entries
            if (ranks.size() < LEADERBOARD_LIMIT) {
                ranks.add(dto);
            }
        }

        return LeaderBoardDto.builder()
            .size(ranks.size())
            .ranks(ranks)
            .ownRank(ownRank)
            .build();
    }

    private static <T> Collection<T> ensureList(Collection<T> c) {
        return c == null ? List.of() : c;
    }
}

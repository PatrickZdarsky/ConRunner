package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.dto.TrustAlertDto;
import at.shiftcontrol.lib.entity.TrustAlert;
import at.shiftcontrol.lib.entity.Volunteer;
import at.shiftcontrol.shiftservice.dao.userprofile.VolunteerDao;
import at.shiftcontrol.shiftservice.dto.TrustAlertDisplayDto;
import at.shiftcontrol.shiftservice.userdirectory.UserInfo;
import at.shiftcontrol.shiftservice.userdirectory.UserLookupService;

@Component
@RequiredArgsConstructor
public class TrustAlertAssemblingMapper {
    private final VolunteerDao volunteerDao;
    private final UserLookupService userDirectoryService;

    public TrustAlertDisplayDto toDto(TrustAlert alert) {
        return toDto(alert, userDirectoryService.getUserById(alert.getVolunteer().getId()));
    }

    private TrustAlertDisplayDto toDto(TrustAlert alert, UserInfo user) {
        return TrustAlertDisplayDto.builder()
            .id(String.valueOf(alert.getId()))
            .volunteerDto(VolunteerAssemblingMapper.toDtoFromUser(user))
            .alertType(alert.getAlertType())
            .alertTypeDescription(alert.getAlertType().getDescription())
            .createdAt(alert.getCreatedAt())
            .build();
    }

    public Collection<TrustAlertDisplayDto> toDto(Collection<TrustAlert> alerts) {
        if (alerts == null) {
            return List.of();
        }

        Map<String, UserInfo> usersById = userDirectoryService.getUserByIds(
            alerts.stream()
                .map(TrustAlert::getVolunteer)
                .map(Volunteer::getId)
                .distinct()
                .toList()
        ).stream().collect(Collectors.toMap(UserInfo::id, Function.identity()));

        return alerts.stream()
            .map(alert -> toDto(
                alert,
                usersById.getOrDefault(alert.getVolunteer().getId(), fallbackUserInfo(alert.getVolunteer().getId()))
            ))
            .toList();
    }

    public TrustAlert toEntity(TrustAlertDto alert) {
        return TrustAlert.builder()
            .volunteer(volunteerDao.getById(alert.getUserId()))
            .alertType(alert.getAlertType())
            .createdAt(alert.getCreatedAt())
            .build();
    }

    private UserInfo fallbackUserInfo(String userId) {
        return new UserInfo(userId, userId, "", "", "", null, false);
    }
}

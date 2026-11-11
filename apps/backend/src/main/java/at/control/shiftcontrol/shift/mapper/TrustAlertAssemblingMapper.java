package at.control.shiftcontrol.shift.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.dto.TrustAlertDto;
import at.control.shiftcontrol.shift.entity.TrustAlert;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.TrustAlertDisplayDto;
import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.identity.UserService;

@Component
@RequiredArgsConstructor
public class TrustAlertAssemblingMapper {
    private final VolunteerDao volunteerDao;
    private final UserService userService;

    public TrustAlertDisplayDto toDto(TrustAlert alert) {
        return toDto(alert, userService.getUser(alert.getVolunteer().getId()));
    }

    private TrustAlertDisplayDto toDto(TrustAlert alert, User user) {
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

        Map<java.util.UUID, User> usersById = userService.getUsers(
            alerts.stream()
                .map(TrustAlert::getVolunteer)
                .map(Volunteer::getId)
                .distinct()
                .toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return alerts.stream()
            .map(alert -> toDto(
                alert,
                usersById.getOrDefault(alert.getVolunteer().getId(), fallbackUser(alert.getVolunteer().getId()))
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

    private User fallbackUser(java.util.UUID userId) {
        return User.builder().id(userId).username(userId.toString()).email("").build();
    }
}

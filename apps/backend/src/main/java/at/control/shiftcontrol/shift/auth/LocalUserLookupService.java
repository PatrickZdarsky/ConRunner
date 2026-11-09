package at.control.shiftcontrol.shift.auth;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.control.shiftcontrol.shift.dto.PaginationDto;
import at.control.shiftcontrol.shift.entity.ExternalIdentity;
import at.control.shiftcontrol.shift.entity.UserAccount;
import at.control.shiftcontrol.shift.lib.exception.BadRequestException;
import at.control.shiftcontrol.shift.dto.user.UserSearchDto;
import at.control.shiftcontrol.shift.mapper.PaginationMapper;
import at.control.shiftcontrol.shift.repo.VolunteerRepository;
import at.control.shiftcontrol.shift.repo.userdirectory.ExternalIdentityRepository;
import at.control.shiftcontrol.shift.repo.userdirectory.UserAccountRepository;
import at.control.shiftcontrol.shift.userdirectory.UserInfo;
import at.control.shiftcontrol.shift.userdirectory.UserLookupService;

@Deprecated
@Service
@Primary
@RequiredArgsConstructor
public class LocalUserLookupService implements UserLookupService {
    private final ExternalIdentityRepository externalIdentityRepository;
    private final UserAccountRepository userAccountRepository;
    private final VolunteerRepository volunteerRepository;

    @Override
    public UserInfo getUserById(String userId) {
        UserInfo user = resolveLocalUsers(Set.of(userId)).get(userId);
        if (user != null) {
            return user;
        }
        if (volunteerRepository.existsById(userId)) {
            return UserInfo.fallback(userId);
        }
        throw new BadRequestException("User not found: " + userId);
    }

    @Override
    public Collection<UserInfo> getUserByIds(Collection<String> userIds) {
        Set<String> requestedIds = userIds.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedIds.isEmpty()) {
            return List.of();
        }

        Map<String, UserInfo> resolvedUsers = resolveLocalUsers(requestedIds);
        Set<String> missingIds = new LinkedHashSet<>(requestedIds);
        missingIds.removeAll(resolvedUsers.keySet());
        volunteerRepository.findAllById(missingIds).stream()
            .map(at.control.shiftcontrol.shift.entity.Volunteer::getId)
            .forEach(userId -> resolvedUsers.put(userId, UserInfo.fallback(userId)));

        return requestedIds.stream()
            .map(resolvedUsers::get)
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public List<UserInfo> getAllUsers() {
        Map<String, UserInfo> usersById = userAccountRepository.findAllWithExternalIdentities().stream()
            .map(this::toUserInfo)
            .collect(Collectors.toMap(UserInfo::id, user -> user, (left, right) -> left, LinkedHashMap::new));

        volunteerRepository.findAll().stream()
            .map(at.control.shiftcontrol.shift.entity.Volunteer::getId)
            .forEach(userId -> usersById.putIfAbsent(userId, UserInfo.fallback(userId)));

        return sort(usersById.values());
    }

    @Override
    public List<UserInfo> getAllAdmins() {
        return sort(userAccountRepository.findAllPlatformAdminsWithExternalIdentities().stream()
            .map(this::toUserInfo)
            .toList());
    }

    @Override
    public @NonNull PaginationDto<UserInfo> searchUsers(int page, int size, @NonNull UserSearchDto searchDto) {
        var accountIdPage = userAccountRepository.searchUserIds(toSearchPattern(searchDto.getName()), PageRequest.of(page, size));
        if (accountIdPage.isEmpty()) {
            return PaginationMapper.toPaginationDto(size, page, accountIdPage.getTotalElements(), List.of());
        }

        List<UserInfo> users = getOrderedUsersByAccountIds(accountIdPage.getContent()).stream()
            .map(this::toUserInfo)
            .toList();
        return PaginationMapper.toPaginationDto(size, page, accountIdPage.getTotalElements(), users);
    }

    private Map<String, UserInfo> resolveLocalUsers(Collection<String> userIds) {
        return externalIdentityRepository.findAllBySubjectIn(userIds).stream()
            .collect(Collectors.groupingBy(
                ExternalIdentity::getSubject,
                LinkedHashMap::new,
                Collectors.collectingAndThen(Collectors.toList(), this::toMostRecentUserInfo)
            ));
    }

    private UserInfo toUserInfo(UserAccount userAccount) {
        ExternalIdentity preferredIdentity = userAccount.getExternalIdentities().stream()
            .max(Comparator.comparing(ExternalIdentity::getLastSeenAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .orElseThrow(() -> new IllegalStateException("User account has no external identities: " + userAccount.getId()));
        return UserInfo.from(preferredIdentity.getSubject(), userAccount);
    }

    private UserInfo toMostRecentUserInfo(List<ExternalIdentity> externalIdentities) {
        ExternalIdentity preferredIdentity = externalIdentities.stream()
            .max(Comparator.comparing(ExternalIdentity::getLastSeenAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .orElse(externalIdentities.get(0));
        return UserInfo.from(preferredIdentity.getSubject(), preferredIdentity.getUserAccount());
    }

    private List<UserAccount> getOrderedUsersByAccountIds(Collection<Long> userAccountIds) {
        Map<Long, UserAccount> accountsById = userAccountRepository.findAllWithExternalIdentitiesByIdIn(userAccountIds).stream()
            .collect(Collectors.toMap(UserAccount::getId, java.util.function.Function.identity()));
        return userAccountIds.stream()
            .map(accountsById::get)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<UserInfo> sort(Collection<UserInfo> users) {
        return users.stream()
            .sorted(Comparator
                .comparing((UserInfo user) -> firstNonBlankOrEmpty(user.lastName(), ""))
                .thenComparing(user -> firstNonBlankOrEmpty(user.firstName(), ""))
                .thenComparing(user -> firstNonBlankOrEmpty(user.username(), user.id())))
            .toList();
    }

    private String toSearchPattern(String name) {
        return name == null || name.isBlank() ? null : "%" + name.trim().toLowerCase() + "%";
    }

    private String firstNonBlankOrEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}

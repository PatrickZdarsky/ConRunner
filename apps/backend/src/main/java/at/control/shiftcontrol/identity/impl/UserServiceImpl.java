package at.control.shiftcontrol.identity.impl;

import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.identity.UserAccountMapper;
import at.control.shiftcontrol.identity.UserSearchRequest;
import at.control.shiftcontrol.identity.UserService;
import at.control.shiftcontrol.identity.entity.UserAccountEntity;
import at.control.shiftcontrol.identity.repo.ExternalIdentityRepository;
import at.control.shiftcontrol.identity.repo.UserAccountRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserAccountRepository userAccountRepository;
    private final ExternalIdentityRepository externalIdentityRepository;

    @Override
    @Transactional(readOnly = true)
    public User getUser(@NonNull String userId) {
        UserAccountEntity account = resolveUsers(Set.of(userId)).get(userId);
        if (account == null) {
            throw new NoSuchElementException("User not found: " + userId);
        }
        return UserAccountMapper.toDto(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<User> getUsers(@NonNull Collection<String> userIds) {
        Set<String> requestedIds = userIds.stream()
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        if (requestedIds.isEmpty()) {
            return List.of();
        }

        Map<String, UserAccountEntity> accountsByUserId = resolveUsers(requestedIds);
        return requestedIds.stream()
            .map(accountsByUserId::get)
            .filter(Objects::nonNull)
            .map(UserAccountMapper::toDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsers(int page, int size, @NonNull UserSearchRequest searchDto) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Long> accountIdPage = userAccountRepository.searchUserIds(toSearchPattern(searchDto.getName()), pageable);
        if (accountIdPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, accountIdPage.getTotalElements());
        }

        Map<Long, UserAccountEntity> accountsById = userAccountRepository
            .findAllWithExternalIdentitiesByIdIn(accountIdPage.getContent())
            .stream()
            .collect(java.util.stream.Collectors.toMap(
                UserAccountEntity::getId,
                java.util.function.Function.identity()));

        List<User> users = accountIdPage.getContent().stream()
            .map(accountsById::get)
            .filter(Objects::nonNull)
            .map(UserAccountMapper::toDto)
            .toList();
        return new PageImpl<>(users, pageable, accountIdPage.getTotalElements());
    }

    private Map<String, UserAccountEntity> resolveUsers(Collection<String> userIds) {
        Map<String, UserAccountEntity> accountsByUserId = new LinkedHashMap<>();
        externalIdentityRepository.findAllBySubjectIn(userIds).forEach(identity ->
            accountsByUserId.putIfAbsent(identity.getSubject(), identity.getUserAccount()));
        return accountsByUserId;
    }

    private static String toSearchPattern(String name) {
        return name == null || name.isBlank() ? null : "%" + name.trim().toLowerCase(Locale.ROOT) + "%";
    }

}

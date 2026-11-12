package at.conrunner.identity;

import lombok.NonNull;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.UUID;

public interface UserService {
    @NonNull
    User getUser(@NonNull UUID userId);

    @NonNull Collection<User> getUsers(@NonNull Collection<UUID> userIds);

    @NonNull Collection<User> getPlatformAdmins();

    @NonNull Page<User> searchUsers(int page, int size, @NonNull UserSearchRequest searchDto);
}

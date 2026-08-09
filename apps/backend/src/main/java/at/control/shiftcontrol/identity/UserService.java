package at.control.shiftcontrol.identity;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;

import java.util.Collection;

public interface UserService {
    @NonNull User getUser(@NonNull String userId);

    @NonNull Collection<User> getUsers(@NonNull Collection<String> userIds);

    @NonNull Page<User> searchUsers(int page, int size, @NonNull UserSearchRequest searchDto);
}

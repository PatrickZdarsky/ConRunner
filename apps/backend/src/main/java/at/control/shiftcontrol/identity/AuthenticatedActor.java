package at.control.shiftcontrol.identity;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AuthenticatedActor {
    ActorType type();
    String name();
    Optional<UUID> userId();
    Collection<? extends GrantedAuthority> authorities();

    default boolean isMachine() {
        return type() == ActorType.MACHINE;
    }
}

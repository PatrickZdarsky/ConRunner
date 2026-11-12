package at.conrunner.identity;

import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AuthenticatedActor extends Serializable {
    ActorType type();
    String name();
    Optional<UUID> userId();
    Collection<GrantedAuthority> authorities();

    default boolean isMachine() {
        return type() == ActorType.MACHINE;
    }
}

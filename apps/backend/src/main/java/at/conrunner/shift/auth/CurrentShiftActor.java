package at.conrunner.shift.auth;

import at.conrunner.identity.AuthenticatedActor;
import at.conrunner.platform.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.NonNull;
import java.util.Optional;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CurrentShiftActor {
    private final UserAttributeProvider userAttributeProvider;

    public Optional<ShiftActor> find() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof AuthenticatedActor actor)) {
            return Optional.empty();
        }
        return Optional.of(new ShiftActor(actor, userAttributeProvider));
    }

    public @NonNull ShiftActor get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authentication found");
        }

        var principle = authentication.getPrincipal();
        //Sanity checks, these should never trip
        if (principle == null) {
            throw new UnauthorizedException("No principle found");
        }
        if (!(principle instanceof AuthenticatedActor)) {
            throw new UnauthorizedException("Principle is not of type AuthenticatedActor");
        }

        return new ShiftActor((AuthenticatedActor) principle, userAttributeProvider);
    }
}

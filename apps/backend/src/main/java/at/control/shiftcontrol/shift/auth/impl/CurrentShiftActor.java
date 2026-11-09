package at.control.shiftcontrol.shift.auth.impl;

import at.control.shiftcontrol.identity.AuthenticatedActor;
import at.control.shiftcontrol.shift.auth.UserAttributeProvider;
import at.control.shiftcontrol.shift.auth.user.ShiftActor;
import at.control.shiftcontrol.shift.lib.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.NonNull;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CurrentShiftActor {
    private final UserAttributeProvider userAttributeProvider;

    public @NonNull ShiftActor get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
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

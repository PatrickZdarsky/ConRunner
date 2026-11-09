package at.control.shiftcontrol.shift.auth.impl;

import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.NonNull;

import at.shiftcontrol.lib.auth.ApplicationUser;
import at.shiftcontrol.lib.auth.UserProvider;
import at.shiftcontrol.lib.exception.UnauthorizedException;

@Component
public class UserProviderImpl implements UserProvider {
    @Override
    public @NonNull ApplicationUser getApplicationUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new UnauthorizedException("No authentication found");
        }

        var principle = authentication.getPrincipal();
        if (principle == null) {
            throw new UnauthorizedException("No principle found");
        }
        if (!(principle instanceof ApplicationUser)) {
            throw new UnauthorizedException("Principle is not of type ApplicationUser");
        }

        return (ApplicationUser) principle;
    }

    @Override
    public Optional<ApplicationUser> getNullableApplicationUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }

        var principle = authentication.getPrincipal();
        if (principle == null) {
            return Optional.empty();
        }
        if (!(principle instanceof ApplicationUser)) {
            return Optional.empty();
        }

        return Optional.of((ApplicationUser) principle);
    }
}

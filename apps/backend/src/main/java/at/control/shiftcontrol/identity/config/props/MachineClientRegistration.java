package at.control.shiftcontrol.identity.config.props;

import java.util.Set;

public record MachineClientRegistration(String clientId, Set<String> authorities) {
    public MachineClientRegistration {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Machine client registrations require a client ID");
        }
        if (authorities == null || authorities.isEmpty() || authorities.stream().anyMatch(authority -> authority == null || authority.isBlank())) {
            throw new IllegalArgumentException("Machine client registrations require at least one authority");
        }
        clientId = clientId.trim();
        authorities = authorities.stream().map(String::trim).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}

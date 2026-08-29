package at.control.shiftcontrol.identity.config.props;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class PlatformAdminBootstrap {
    private final Set<ExternalIdentity> identities;

    public PlatformAdminBootstrap(Collection<ExternalIdentity> identities) {
        this.identities = Set.copyOf(identities == null ? Set.of() : new HashSet<>(identities));
    }

    public static PlatformAdminBootstrap none() {
        return new PlatformAdminBootstrap(Set.of());
    }

    public boolean matches(String issuer, String subject) {
        return identities.contains(new ExternalIdentity(issuer, subject));
    }

    public record ExternalIdentity(String issuer, String subject) {
        public ExternalIdentity {
            if (isBlank(issuer) || isBlank(subject)) {
                throw new IllegalArgumentException("Bootstrap platform administrators require issuer and subject");
            }
            issuer = issuer.trim();
            subject = subject.trim();
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }
}

package at.conrunner.identity.config.props;

import java.util.Set;

public record OidcProviderProps(
    String issuerUri,
    String jwkSetUri,
    Set<String> allowedIssuers,
    String audience,
    String usernameClaim
) {}

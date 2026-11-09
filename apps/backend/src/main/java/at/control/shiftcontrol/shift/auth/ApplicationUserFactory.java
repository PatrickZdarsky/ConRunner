package at.control.shiftcontrol.shift.auth;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import at.shiftcontrol.shiftservice.auth.user.HumanUser;
import at.shiftcontrol.shiftservice.auth.user.ServiceUser;
import at.shiftcontrol.shiftservice.auth.user.ShiftControlUser;
@Deprecated
public class ApplicationUserFactory {
    public static ShiftControlUser createHumanUser(
        String username,
        String userId,
        Set<String> scopeAuthorities,
        UserAttributeProvider attributeProvider,
        boolean platformAdmin
    ) {
        List<SimpleGrantedAuthority> grantedAuthorities = scopeAuthorities.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        if (platformAdmin && grantedAuthorities.stream().noneMatch(a -> "ADMIN".equals(a.getAuthority()))) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ADMIN"));
        }

        return new HumanUser(
            grantedAuthorities,
            username,
            userId,
            attributeProvider,
            platformAdmin
        );
    }

    public static ShiftControlUser createServiceUser(String username, String userId, Set<String> scopeAuthorities) {
        return new ServiceUser(
            scopeAuthorities.isEmpty()
                ? Collections.emptyList()
                : scopeAuthorities.stream().map(SimpleGrantedAuthority::new).toList(),
            username,
            userId
        );
    }
}

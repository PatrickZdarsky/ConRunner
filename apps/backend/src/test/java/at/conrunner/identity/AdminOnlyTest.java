package at.conrunner.identity;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.AuthorityUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminOnlyTest {
    @ParameterizedTest
    @CsvSource({"admin, true", "ADMIN, true", "AdMiN, true", "shiftservice.users.read, false", "'', false"})
    void checksPlatformAdminAuthorityIgnoringCase(String authority, boolean granted) throws NoSuchMethodException {
        var invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(AdminActions.class.getMethod("manage"));
        when(invocation.getThis()).thenReturn(new AdminActions());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
            "test", null, AuthorityUtils.commaSeparatedStringToAuthorityList(authority));

        var result = new PreAuthorizeAuthorizationManager().authorize(() -> authentication, invocation);

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isEqualTo(granted);
    }

    static class AdminActions {
        @AdminOnly
        public void manage() {
            // The test invokes authorization for this annotated method only.
        }
    }
}

package at.conrunner.shift.endpoint;

import at.conrunner.shift.auth.CurrentShiftActor;
import at.conrunner.shift.auth.ShiftActor;
import at.conrunner.shift.dto.userprofile.UserProfileDto;
import at.conrunner.shift.service.userprofile.UserProfileService;
import at.conrunner.shift.service.userprofile.VolunteerCalendarService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = UserProfileEndpoint.class,
    properties = "spring.mvc.servlet.path=/",
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "at\\.shiftcontrol\\.shiftservice\\.config\\.TraceIdFilter"
    )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserProfileEndpointRootServletPathTest.TestConfig.class)
class UserProfileEndpointRootServletPathTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${spring.mvc.servlet.path}")
    private String servletPath;

    @Test
    void shouldExposeEndpointWithoutAdditionalPrefix() throws Exception {
        Assertions.assertEquals("/", servletPath);

        mockMvc.perform(get("/api/v1/me/profile"))
            .andExpect(status().isOk());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        CurrentShiftActor currentShiftActor() {
            var provider = org.mockito.Mockito.mock(CurrentShiftActor.class);
            var actor = org.mockito.Mockito.mock(ShiftActor.class);
            org.mockito.Mockito.when(provider.get()).thenReturn(actor);
            org.mockito.Mockito.when(actor.requireUserId()).thenReturn(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
            return provider;
        }

        @Bean
        UserProfileService userProfileService() {
            return userId -> UserProfileDto.builder()
                .assignedRoles(List.of()).planningPlans(List.of()).volunteeringPlans(List.of())
                .planningEvents(List.of()).volunteeringEvents(List.of()).build();
        }

        @Bean
        VolunteerCalendarService volunteerCalendarService() {
            return org.mockito.Mockito.mock(VolunteerCalendarService.class);
        }
    }
}

package at.control.shiftcontrol.shift.endpoint;

import at.control.shiftcontrol.shift.auth.CurrentShiftActor;
import at.control.shiftcontrol.shift.auth.ShiftActor;
import at.control.shiftcontrol.shift.dto.userprofile.UserProfileDto;
import at.control.shiftcontrol.shift.service.userprofile.UserProfileService;
import at.control.shiftcontrol.shift.service.userprofile.VolunteerCalendarService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
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
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = UserProfileEndpoint.class,
    properties = "spring.mvc.servlet.path=/shiftservice",
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "at\\.shiftcontrol\\.shiftservice\\.config\\.TraceIdFilter"
    )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserProfileEndpointPathPrefixTest.TestConfig.class)
class UserProfileEndpointPathPrefixTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${spring.mvc.servlet.path}")
    private String servletPath;

    @Test
    void shouldExposeEndpointUnderConfiguredPrefix() throws Exception {
        Assertions.assertEquals("/shiftservice", servletPath);

        mockMvc.perform(get("/shiftservice/api/v1/me/profile")
                .servletPath("/shiftservice"))
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

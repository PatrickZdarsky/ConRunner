package at.control.shiftcontrol.shift.endpoint;

import at.control.shiftcontrol.shift.auth.CurrentShiftActor;
import at.control.shiftcontrol.shift.auth.ShiftActor;
import at.control.shiftcontrol.shift.dto.userprofile.CalendarShareIdDto;
import at.control.shiftcontrol.shift.dto.userprofile.UserProfileDto;
import at.control.shiftcontrol.shift.service.userprofile.UserProfileService;
import at.control.shiftcontrol.shift.service.userprofile.VolunteerCalendarService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class UserProfileEndpointTest {
    private static final java.util.UUID CURRENT_USER_ID = java.util.UUID.randomUUID();
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    @Mock
    private CurrentShiftActor userProvider;
    @Mock
    private UserProfileService service;
    @Mock
    private VolunteerCalendarService volunteerCalendarService;
    @InjectMocks
    private UserProfileEndpoint endpoint;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(endpoint).build();
    }

    @Test
    void getCurrentUserProfile_ShouldReturnUserProfileDto() throws Exception {
        // mock the user object
        var currentUser = mock(ShiftActor.class);
        when(userProvider.get()).thenReturn(currentUser);
        when(currentUser.requireUserId()).thenReturn(CURRENT_USER_ID);

        // mock the service
        var userProfileDto = mock(UserProfileDto.class);
        when(service.getUserProfile(CURRENT_USER_ID)).thenReturn(userProfileDto);

        mockMvc
            .perform(MockMvcRequestBuilders
                .get("/api/v1/me/profile")
                .accept(MediaType.APPLICATION_JSON)
            ).andExpect(status().isOk())
            .andExpect(content().string(objectMapper.writeValueAsString(userProfileDto)));

        verify(service).getUserProfile(CURRENT_USER_ID);
    }

    @Test
    void regenerateCalendarShareId_ShouldReturnCalendarShareIdDto() throws Exception {
        var currentUser = mock(ShiftActor.class);
        when(userProvider.get()).thenReturn(currentUser);
        when(currentUser.requireUserId()).thenReturn(CURRENT_USER_ID);

        var shareIdDto = CalendarShareIdDto.builder()
            .calendarShareId("NEWCALENDARSHAREID123")
            .build();
        when(volunteerCalendarService.regenerateCalendarShareId(CURRENT_USER_ID)).thenReturn(shareIdDto);

        mockMvc
            .perform(MockMvcRequestBuilders
                .post("/api/v1/me/profile/calendar-share-id/regenerate")
                .accept(MediaType.APPLICATION_JSON)
            ).andExpect(status().isOk())
            .andExpect(content().string(objectMapper.writeValueAsString(shareIdDto)));

        verify(volunteerCalendarService).regenerateCalendarShareId(CURRENT_USER_ID);
    }
}

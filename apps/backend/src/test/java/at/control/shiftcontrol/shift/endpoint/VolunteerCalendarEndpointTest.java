package at.control.shiftcontrol.shift.endpoint;

import at.control.shiftcontrol.shift.service.userprofile.VolunteerCalendarService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VolunteerCalendarEndpointTest {
    private MockMvc mockMvc;

    @Mock
    private VolunteerCalendarService volunteerCalendarService;

    @InjectMocks
    private VolunteerCalendarEndpoint endpoint;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(endpoint).build();
    }

    @Test
    void getVolunteerCalendar_returnsIcsFeed() throws Exception {
        String shareId = "CALENDAR123";
        String calendar = "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n";
        when(volunteerCalendarService.getCalendar(shareId)).thenReturn(calendar);

        mockMvc.perform(MockMvcRequestBuilders
                .get("/api/v1/calendar/share/{shareId}", shareId)
                .accept(MediaType.parseMediaType("text/calendar")))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/calendar;charset=UTF-8"))
            .andExpect(header().string("Content-Disposition", "inline; filename=shiftcontrol-shifts.ics"))
            .andExpect(content().string(calendar));

        verify(volunteerCalendarService).getCalendar(shareId);
    }
}

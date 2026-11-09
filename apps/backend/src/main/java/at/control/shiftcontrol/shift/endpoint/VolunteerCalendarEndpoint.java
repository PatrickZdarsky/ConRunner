package at.control.shiftcontrol.shift.endpoint;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import at.shiftcontrol.shiftservice.service.userprofile.VolunteerCalendarService;

@RestController
@RequestMapping("api/v1/calendar")
@RequiredArgsConstructor
public class VolunteerCalendarEndpoint {
    private static final MediaType CALENDAR_MEDIA_TYPE = MediaType.parseMediaType("text/calendar;charset=UTF-8");

    private final VolunteerCalendarService volunteerCalendarService;

    @GetMapping(value = "/share/{shareId}", produces = "text/calendar;charset=UTF-8")
    @Operation(
        operationId = "getVolunteerCalendarByShareId",
        description = "Get an iCalendar feed for volunteer assignments using a share id"
    )
    public ResponseEntity<String> getVolunteerCalendar(@PathVariable String shareId) {
        String calendar = volunteerCalendarService.getCalendar(shareId);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=shiftcontrol-shifts.ics")
            .contentType(CALENDAR_MEDIA_TYPE)
            .contentLength(calendar.getBytes(StandardCharsets.UTF_8).length)
            .body(calendar);
    }
}

package at.control.shiftcontrol.shift.service.userprofile.impl;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import at.control.shiftcontrol.shift.util.UniqueCodeGenerator;
import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.Location;
import at.control.shiftcontrol.shift.entity.Shift;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.lib.exception.NotFoundException;
import at.control.shiftcontrol.shift.lib.type.AssignmentStatus;
import at.control.shiftcontrol.shift.dao.userprofile.VolunteerDao;
import at.control.shiftcontrol.shift.dto.userprofile.CalendarShareIdDto;
import at.control.shiftcontrol.shift.repo.AssignmentRepository;
import at.control.shiftcontrol.shift.service.userprofile.VolunteerCalendarService;

@Service
@RequiredArgsConstructor
public class VolunteerCalendarServiceImpl implements VolunteerCalendarService {
    private static final String SHARE_ID_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SHARE_ID_LENGTH = 24;
    private static final int MAX_SHARE_ID_GENERATION_ATTEMPTS = 10;
    private static final DateTimeFormatter ICS_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        .withZone(ZoneOffset.UTC);
    private static final EnumSet<AssignmentStatus> CALENDAR_ASSIGNMENT_STATUSES =
        EnumSet.of(AssignmentStatus.ACCEPTED, AssignmentStatus.AUCTION, AssignmentStatus.AUCTION_REQUEST_FOR_UNASSIGN);

    private final VolunteerDao volunteerDao;
    private final AssignmentRepository assignmentRepository;
    private final UniqueCodeGenerator uniqueCodeGenerator;

    @Override
    @Transactional
    public Optional<String> getOrCreateCalendarShareId(String userId) {
        return volunteerDao.findById(userId)
            .map(volunteer -> {
                if (volunteer.getCalendarShareId() != null && !volunteer.getCalendarShareId().isBlank()) {
                    return volunteer.getCalendarShareId();
                }

                return assignAndPersistShareId(volunteer);
            });
    }

    @Override
    @Transactional
    public CalendarShareIdDto regenerateCalendarShareId(String userId) {
        Volunteer volunteer = volunteerDao.findById(userId)
            .orElseThrow(() -> new NotFoundException("Volunteer not found"));

        String calendarShareId = assignAndPersistShareId(volunteer);
        return CalendarShareIdDto.builder()
            .calendarShareId(calendarShareId)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String getCalendar(String shareId) {
        Volunteer volunteer = volunteerDao.findByCalendarShareId(shareId)
            .orElseThrow(() -> new NotFoundException("Volunteer calendar not found"));
        Collection<Assignment> assignments = assignmentRepository.findCalendarAssignmentsByVolunteerId(
            volunteer.getId(),
            CALENDAR_ASSIGNMENT_STATUSES
        );

        return buildCalendar(assignments, volunteer.getId());
    }

    private String assignAndPersistShareId(Volunteer volunteer) {
        volunteer.setCalendarShareId(generateShareId());

        try {
            volunteerDao.save(volunteer);
        } catch (DataIntegrityViolationException e) {
            volunteer.setCalendarShareId(generateShareId());
            volunteerDao.save(volunteer);
        }

        return volunteer.getCalendarShareId();
    }

    private String generateShareId() {
        return uniqueCodeGenerator.generateUnique(
            SHARE_ID_ALPHABET,
            SHARE_ID_LENGTH,
            MAX_SHARE_ID_GENERATION_ATTEMPTS,
            volunteerDao::existsByCalendarShareId
        );
    }

    private String buildCalendar(Collection<Assignment> assignments, String volunteerId) {
        Instant now = Instant.now();
        StringBuilder builder = new StringBuilder();

        appendLine(builder, "BEGIN:VCALENDAR");
        appendLine(builder, "VERSION:2.0");
        appendLine(builder, "PRODID:-//ShiftControl//Volunteer Calendar//EN");
        appendLine(builder, "CALSCALE:GREGORIAN");
        appendLine(builder, "METHOD:PUBLISH");
        appendLine(builder, "X-WR-CALNAME:ShiftControl Shifts");

        for (CalendarShiftEntry entry : aggregateAssignments(assignments, volunteerId)) {
            appendLine(builder, "BEGIN:VEVENT");
            appendLine(builder, "UID:" + escapeText(entry.uid()));
            appendLine(builder, "DTSTAMP:" + formatDateTime(now));
            appendLine(builder, "DTSTART:" + formatDateTime(entry.startTime()));
            appendLine(builder, "DTEND:" + formatDateTime(entry.endTime()));
            appendLine(builder, "SUMMARY:" + escapeText(entry.summary()));
            appendLine(builder, "DESCRIPTION:" + escapeText(entry.description()));
            if (entry.location() != null && !entry.location().isBlank()) {
                appendLine(builder, "LOCATION:" + escapeText(entry.location()));
            }
            if (entry.url() != null && !entry.url().isBlank()) {
                appendLine(builder, "URL:" + escapeText(entry.url()));
            }
            appendLine(builder, "STATUS:CONFIRMED");
            appendLine(builder, "END:VEVENT");
        }

        appendLine(builder, "END:VCALENDAR");
        return builder.toString();
    }

    private Collection<CalendarShiftEntry> aggregateAssignments(Collection<Assignment> assignments, String volunteerId) {
        LinkedHashMap<Long, CalendarShiftAccumulator> shifts = new LinkedHashMap<>();

        for (Assignment assignment : assignments) {
            Shift shift = assignment.getPositionSlot().getShift();
            CalendarShiftAccumulator accumulator = shifts.computeIfAbsent(
                shift.getId(),
                ignored -> new CalendarShiftAccumulator(shift, volunteerId)
            );
            accumulator.addAssignment(assignment);
        }

        return shifts.values().stream()
            .map(CalendarShiftAccumulator::toEntry)
            .toList();
    }

    private String formatDateTime(Instant instant) {
        return ICS_DATE_TIME.format(instant);
    }

    private void appendLine(StringBuilder builder, String line) {
        builder.append(line).append("\r\n");
    }

    private String escapeText(String input) {
        return input
            .replace("\\", "\\\\")
            .replace("\r\n", "\n")
            .replace("\n", "\\n")
            .replace(";", "\\;")
            .replace(",", "\\,");
    }

    private record CalendarShiftEntry(
        String uid,
        Instant startTime,
        Instant endTime,
        String summary,
        String description,
        String location,
        String url
    ) {
    }

    private static final class CalendarShiftAccumulator {
        private final Shift shift;
        private final String volunteerId;
        private final List<String> slotNames = new ArrayList<>();

        private CalendarShiftAccumulator(Shift shift, String volunteerId) {
            this.shift = shift;
            this.volunteerId = volunteerId;
        }

        private void addAssignment(Assignment assignment) {
            slotNames.add(assignment.getPositionSlot().getName());
        }

        private CalendarShiftEntry toEntry() {
            Location location = shift.getLocation();
            String summary = "%s - %s".formatted(shift.getShiftPlan().getEvent().getName(), shift.getName());
            String description = buildDescription(location);

            return new CalendarShiftEntry(
                "shift-%d-volunteer-%s@shiftcontrol".formatted(shift.getId(), volunteerId),
                shift.getStartTime(),
                shift.getEndTime(),
                summary,
                description,
                location == null ? null : location.getName(),
                location == null ? null : location.getUrl()
            );
        }

        private String buildDescription(Location location) {
            List<String> lines = new ArrayList<>();
            lines.add("Event: " + shift.getShiftPlan().getEvent().getName());
            lines.add("Shift plan: " + shift.getShiftPlan().getName());
            lines.add("Shift: " + shift.getName());

            String slotLine = slotNames.stream()
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
            if (!slotLine.isBlank()) {
                lines.add("Assigned slots: " + slotLine);
            }

            if (shift.getShortDescription() != null && !shift.getShortDescription().isBlank()) {
                lines.add("Shift details: " + shift.getShortDescription());
            }

            if (location != null && location.getDescription() != null && !location.getDescription().isBlank()) {
                lines.add("Location details: " + location.getDescription());
            }

            if (location != null && location.getUrl() != null && !location.getUrl().isBlank()) {
                lines.add("Location link: " + location.getUrl());
            }

            return String.join("\n", lines);
        }
    }
}

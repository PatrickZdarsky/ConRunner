package at.conrunner.shift.service.rewardpoints.impl;

import at.conrunner.audit.api.AuditRecorder;
import at.conrunner.shift.util.UniqueCodeGenerator;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.dao.EventDao;
import at.conrunner.shift.dao.RewardPointsShareTokenDao;
import at.conrunner.shift.dao.RewardPointsTransactionDao;
import at.conrunner.shift.dao.userprofile.VolunteerDao;
import at.conrunner.shift.service.rewardpoints.RewardPointsCalculator;
import at.conrunner.shift.service.rewardpoints.RewardPointsLedgerService;
import at.conrunner.identity.User;
import at.conrunner.identity.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardPointsServiceImplTest {
    @Mock
    AuditRecorder auditRecorder;
    @Mock
    private RewardPointsCalculator calculator;

    @Mock
    private RewardPointsLedgerService ledgerService;

    @Mock
    private RewardPointsShareTokenDao rewardPointsShareTokenDao;

    @Mock
    private EventDao eventDao;

    @Mock
    private VolunteerDao volunteerDao;

    @Mock
    private RewardPointsTransactionDao rewardPointsTransactionDao;

    @Mock
    private UserService userDirectoryService;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private UniqueCodeGenerator uniqueCodeGenerator;

    @InjectMocks
    private RewardPointsServiceImpl rewardPointsService;

    @Test
    void getRewardPointsForAllUsersOverAllEvents_usesDirectoryProfilesForExportRows() {
        var event = new Event();
        event.setId(12L);
        event.setName("Conference");
        event.setStartTime(Instant.parse("2026-01-01T10:00:00Z"));
        event.setEndTime(Instant.parse("2026-01-02T10:00:00Z"));

        var volunteer = new Volunteer();
        var userId = java.util.UUID.randomUUID();
        volunteer.setId(userId);

        when(eventDao.findAll()).thenReturn(List.of(event));
        when(volunteerDao.findAllByEvent(12L)).thenReturn(List.of(volunteer));
        when(userDirectoryService.getUsers(List.of(userId))).thenReturn(List.of(
            User.builder().id(userId).username("Casey").email("casey@example.com").build()
        ));
        when(rewardPointsTransactionDao.sumPointsByVolunteerAndEvent(userId.toString(), 12L)).thenReturn(42L);

        var result = rewardPointsService.getRewardPointsForAllUsersOverAllEvents();

        assertThat(result).hasSize(1);
        var export = result.iterator().next();
        assertThat(export.getEvent().getName()).isEqualTo("Conference");
        assertThat(export.getVolunteerPoints()).hasSize(1);
        var volunteerPoints = export.getVolunteerPoints().iterator().next();
        assertThat(volunteerPoints.getVolunteerId()).isEqualTo(userId.toString());
        assertThat(volunteerPoints.getName()).isEqualTo("Casey");
        assertThat(volunteerPoints.getEmail()).isEqualTo("casey@example.com");
        assertThat(volunteerPoints.getRewardPoints()).isEqualTo(42);
    }

}

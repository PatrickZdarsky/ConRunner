package at.conrunner.shift.integration;

import static org.assertj.core.api.Assertions.assertThat;

import at.conrunner.shift.dto.PaginationDto;
import at.conrunner.shift.dto.TrustAlertDto;
import at.conrunner.shift.entity.TrustAlert;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.lib.type.TrustAlertType;
import at.conrunner.shift.dto.TrustAlertDisplayDto;
import at.conrunner.shift.integration.config.RestITBase;
import at.conrunner.shift.repo.TrustAlertRepository;
import at.conrunner.shift.repo.VolunteerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrustAlertIT extends RestITBase {
    private static final String TRUST_ALERT_PATH = "/trust-alerts";
    private static final String TRUST_ALERT_QUERY_PARAMS = "?page=%d&size=%d";

    @Autowired
    VolunteerRepository volunteerRepository;
    @Autowired
    TrustAlertRepository trustAlertRepository;

    private Volunteer volunteerA;
    private TrustAlert trustAlertA, trustAlertB;

    @BeforeEach
    void setUp() {

        volunteerA = Volunteer.builder()
            .id(java.util.UUID.fromString("00000000-0000-0000-0000-000000011111"))
            .build();
        volunteerA = volunteerRepository.save(volunteerA);

        trustAlertA = TrustAlert.builder()
            .alertType(TrustAlertType.OVERLOAD)
            .createdAt(Instant.now())
            .volunteer(volunteerA)
            .build();
        trustAlertA = trustAlertRepository.save(trustAlertA);
        trustAlertB = TrustAlert.builder()
            .alertType(TrustAlertType.SPAM)
            .createdAt(Instant.now())
            .volunteer(volunteerA)
            .build();
        trustAlertB = trustAlertRepository.save(trustAlertB);
    }

    @Test
    void getTrustAlertAsAdminSucceeds() {
        long page = 0;
        long size = 1;
        var result = getRequestAsAdmin(
            TRUST_ALERT_PATH + TRUST_ALERT_QUERY_PARAMS.formatted(page, size),
            PaginationDto.class
        );
        assertThat(result).isNotNull();
        assertFalse(result.getItems().isEmpty());
        TrustAlertDisplayDto alert = objectMapper.convertValue(result.getItems().stream().findFirst().get(), TrustAlertDisplayDto.class);
        assertAll(
            () -> assertEquals(trustAlertB.getAlertType(), alert.getAlertType()),
            () -> assertEquals(trustAlertB.getAlertType().getDescription(), alert.getAlertType().getDescription()),
            () -> assertTrue(Math.abs(
                trustAlertB.getCreatedAt().toEpochMilli() - alert.getCreatedAt().toEpochMilli()) < 1),
            () -> assertEquals(trustAlertB.getVolunteer().getId(), alert.getVolunteerDto().getId())
        );
    }

    @Test
    void postTrustAlertAsAdminSucceeds() {
        TrustAlertDto trustAlertDto = TrustAlertDto.builder()
            .alertType(TrustAlertType.OVERLOAD)
            .createdAt(Instant.now())
            .userId(volunteerA.getId())
            .build();

        var result = postRequestAsAdmin(
            TRUST_ALERT_PATH,
            trustAlertDto,
            TrustAlertDisplayDto.class);

        assertAll(
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getId()).isNotNull(),
            () -> assertEquals(trustAlertDto.getCreatedAt(), result.getCreatedAt()),
            () -> assertEquals(trustAlertDto.getAlertType(), result.getAlertType()),
            () -> assertEquals(trustAlertDto.getUserId(), result.getVolunteerDto().getId())
        );
    }

    @Test
    void deleteTrustAlertAsAdminSucceeds() {
        deleteRequestAsAdmin(
            TRUST_ALERT_PATH + "/" + trustAlertA.getId()
        );
        assertFalse(trustAlertRepository.findById(trustAlertA.getId()).isPresent());
    }
}

package at.conrunner.shift.dao.impl;

import at.conrunner.shift.entity.Activity;
import at.conrunner.shift.entity.Assignment;
import at.conrunner.shift.entity.AssignmentSwitchRequest;
import at.conrunner.shift.entity.Event;
import at.conrunner.shift.entity.ExternalIdentity;
import at.conrunner.shift.entity.Location;
import at.conrunner.shift.entity.PositionConstraint;
import at.conrunner.shift.entity.PositionSlot;
import at.conrunner.shift.entity.PositionSlotPreference;
import at.conrunner.shift.entity.PretalxApiKey;
import at.conrunner.shift.entity.RewardPointsShareToken;
import at.conrunner.shift.entity.RewardPointsTransaction;
import at.conrunner.shift.entity.Role;
import at.conrunner.shift.entity.Shift;
import at.conrunner.shift.entity.ShiftPlan;
import at.conrunner.shift.entity.ShiftPlanInvite;
import at.conrunner.shift.entity.SocialMediaLink;
import at.conrunner.shift.entity.TimeConstraint;
import at.conrunner.shift.entity.TrustAlert;
import at.conrunner.shift.entity.UserAccount;
import at.conrunner.shift.entity.Volunteer;
import at.conrunner.shift.entity.*;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;

class VolunteerPersistenceTest {
    @Test
    void uuidIdRoundTripsThroughNativeUuidColumn() throws Exception {
        try (var db = new PostgreSQLContainer("postgres:16-alpine")) {
            db.start();
            try (var conn = DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword()); var statement = conn.createStatement()) {
                statement.execute("create table volunteer (id uuid primary key, calendar_share_id varchar(64))");
            }
            var registry = new StandardServiceRegistryBuilder().applySetting("hibernate.connection.url", db.getJdbcUrl())
                .applySetting("hibernate.connection.username", db.getUsername()).applySetting("hibernate.connection.password", db.getPassword())
                .applySetting("hibernate.connection.driver_class", "org.postgresql.Driver").build();
            try {
                var sources = new MetadataSources(registry);
                for (Class<?> entity : List.of(PretalxApiKey.class, RewardPointsShareToken.class, AssignmentSwitchRequest.class, Volunteer.class,
                        TimeConstraint.class, RewardPointsTransaction.class, Assignment.class, ShiftPlanInvite.class, TrustAlert.class,
                        PositionConstraint.class, SocialMediaLink.class, UserAccount.class, ExternalIdentity.class, PositionSlotPreference.class,
                        Shift.class, PositionSlot.class, Location.class, ShiftPlan.class, Activity.class, Event.class, Role.class)) sources.addAnnotatedClass(entity);
                try (var factory = sources.buildMetadata().buildSessionFactory()) {
                    var id = UUID.randomUUID();
                    try (var session = factory.openSession()) {
                        session.beginTransaction();
                        session.persist(Volunteer.builder().id(id).build());
                        session.getTransaction().commit();
                    }
                    try (var session = factory.openSession()) {
                        assertThat(session.find(Volunteer.class, id).getId()).isEqualTo(id);
                        assertThat(session.createQuery("from Volunteer v where v.id = :id", Volunteer.class)
                            .setParameter("id", id).getSingleResult().getId()).isEqualTo(id);
                    }
                    try (var conn = DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword()); var statement = conn.createStatement(); var result = statement.executeQuery("select id from volunteer")) {
                        assertThat(result.next()).isTrue();
                        assertThat(result.getObject(1, UUID.class)).isEqualTo(id);
                    }
                }
            } finally { StandardServiceRegistryBuilder.destroy(registry); }
        }
    }
}

package at.control.shiftcontrol.communications.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

interface NotificationRevisionRepository extends JpaRepository<NotificationRevisionEntity, UUID> {
    @Modifying
    @Query(value = """
        insert into communications_notification_revision (recipient_id, revision) values (:userId, 1)
        on conflict (recipient_id) do update set revision = communications_notification_revision.revision + 1
        """, nativeQuery = true)
    void increment(@Param("userId") UUID userId);

    @Query(value = "select revision from communications_notification_revision where recipient_id = :userId", nativeQuery = true)
    java.util.Optional<Long> revision(@Param("userId") UUID userId);
}

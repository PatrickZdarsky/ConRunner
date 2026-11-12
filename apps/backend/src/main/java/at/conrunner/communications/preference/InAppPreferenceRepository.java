package at.conrunner.communications.preference;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

interface InAppPreferenceRepository extends JpaRepository<InAppPreferenceEntity, UUID> {
    Optional<InAppPreferenceEntity> findByRecipientIdAndType(UUID recipientId, String type);
    @Modifying
    @Query(value = """
        insert into communications_in_app_preference (id, recipient_id, notification_type, enabled)
        values (:id, :recipientId, :type, :enabled)
        on conflict (recipient_id, notification_type) do update set enabled = excluded.enabled
        """, nativeQuery = true)
    int setEnabled(@Param("id") UUID id, @Param("recipientId") UUID recipientId,
                   @Param("type") String type, @Param("enabled") boolean enabled);
}

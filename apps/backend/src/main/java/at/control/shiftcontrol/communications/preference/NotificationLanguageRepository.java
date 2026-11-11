package at.control.shiftcontrol.communications.preference;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

interface NotificationLanguageRepository extends JpaRepository<NotificationLanguageEntity, UUID> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        insert into communications_notification_language (recipient_id, language) values (:id, :language)
        on conflict (recipient_id) do update set language = excluded.language
        """, nativeQuery = true)
    int setLanguage(@Param("id") UUID id, @Param("language") String language);
}

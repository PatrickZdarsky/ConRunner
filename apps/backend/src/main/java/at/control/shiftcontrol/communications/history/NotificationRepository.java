package at.control.shiftcontrol.communications.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.UUID;

interface NotificationRepository extends JpaRepository<NotificationEntity, UUID>, JpaSpecificationExecutor<NotificationEntity> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update NotificationEntity n set n.readAt = :readAt where n.id = :id and n.recipientId = :recipientId")
    int setReadAt(@Param("id") UUID id, @Param("recipientId") UUID recipientId, @Param("readAt") Instant readAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update NotificationEntity n set n.readAt = :readAt where n.recipientId = :recipientId and n.readAt is null")
    int markAllRead(@Param("recipientId") UUID recipientId, @Param("readAt") Instant readAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from NotificationEntity n where n.id = :id and n.recipientId = :recipientId")
    int deleteOwned(@Param("id") UUID id, @Param("recipientId") UUID recipientId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from NotificationEntity n where n.recipientId = :recipientId")
    int deleteAllOwned(@Param("recipientId") UUID recipientId);

    @Modifying
    @Query(value = """
        insert into communications_notification
            (id, request_id, recipient_id, notification_type, title, body, link_target, created_at)
        values (:id, :requestId, :recipientId, :type, :title, :body, :link, current_timestamp)
        on conflict (request_id) do nothing
        """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("requestId") UUID requestId,
        @Param("recipientId") UUID recipientId, @Param("type") String type, @Param("title") String title,
        @Param("body") String body, @Param("link") String link);
}

package at.conrunner.communications.email;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface EmailDeliveryRepository extends JpaRepository<EmailDeliveryEntity, UUID> {
    @Modifying
    @Query(value = """
        insert into communications_email_delivery
            (id, request_id, recipient_id, notification_type, title, body, link_target, status, attempts, available_at)
        values (:id, :requestId, :recipientId, :type, :title, :body, :link, 'PENDING', 0, current_timestamp)
        on conflict (request_id) do nothing
        """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("requestId") UUID requestId,
        @Param("recipientId") UUID recipientId, @Param("type") String type,
        @Param("title") String title, @Param("body") String body, @Param("link") String link);

    @Query(value = """
        select * from communications_email_delivery
        where (status = 'PENDING' and available_at <= current_timestamp)
           or (status = 'PROCESSING' and lease_until <= current_timestamp)
        order by available_at, id limit :limit for update skip locked
        """, nativeQuery = true)
    List<EmailDeliveryEntity> lockAvailable(@Param("limit") int limit);

    @Query(value = "select current_timestamp", nativeQuery = true)
    Instant databaseTime();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        update communications_email_delivery set status = :status, lease_id = null, lease_until = null
        where id = :id and status = 'PROCESSING' and lease_id = :leaseId and lease_until > current_timestamp
        """, nativeQuery = true)
    int finish(@Param("id") UUID id, @Param("leaseId") UUID leaseId, @Param("status") String status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        update communications_email_delivery set status = case when attempts >= 5 then 'FAILED' else 'PENDING' end,
          available_at = current_timestamp + interval '1 minute', lease_id = null, lease_until = null
        where id = :id and status = 'PROCESSING' and lease_id = :leaseId and lease_until > current_timestamp
        """, nativeQuery = true)
    int retry(@Param("id") UUID id, @Param("leaseId") UUID leaseId);
}

package at.conrunner.communications.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface NotificationRequestRepository extends JpaRepository<NotificationRequestEntity, UUID> {
    @Modifying
    @Query(value = """
        insert into communications_request
            (id, trigger_id, recipient_id, notification_type, template_data, created_at,
             status, attempts, available_at, email_requested, in_app_requested)
        values (:id, :triggerId, :recipientId, :type, :data, current_timestamp,
                'PENDING', 0, current_timestamp, :emailRequested, :inAppRequested)
        on conflict (trigger_id, recipient_id, notification_type) do nothing
        """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("triggerId") UUID triggerId,
                       @Param("recipientId") UUID recipientId, @Param("type") String type, @Param("data") String data,
                       @Param("emailRequested") boolean emailRequested, @Param("inAppRequested") boolean inAppRequested);

    @Query(value = """
        select * from communications_request
        where (status = 'PENDING' and available_at <= current_timestamp)
           or (status = 'PROCESSING' and lease_until <= current_timestamp)
        order by created_at, id limit :limit for update skip locked
        """, nativeQuery = true)
    List<NotificationRequestEntity> lockAvailable(@Param("limit") int limit);

    @Query(value = "select current_timestamp", nativeQuery = true)
    Instant databaseTime();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        update communications_request set status = 'COMPLETED', lease_id = null, lease_until = null
        where id = :id and status = 'PROCESSING' and lease_id = :leaseId and lease_until > current_timestamp
        """, nativeQuery = true)
    int complete(@Param("id") UUID id, @Param("leaseId") UUID leaseId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        update communications_request
        set status = case when attempts >= :maxAttempts then 'FAILED' else 'PENDING' end,
            available_at = :availableAt, lease_id = null, lease_until = null
        where id = :id and status = 'PROCESSING' and lease_id = :leaseId and lease_until > current_timestamp
        """, nativeQuery = true)
    int retry(@Param("id") UUID id, @Param("leaseId") UUID leaseId,
              @Param("availableAt") Instant availableAt, @Param("maxAttempts") int maxAttempts);
}

package at.conrunner.platform.realtime.socket;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

interface WebSocketTicketRepository extends JpaRepository<WebSocketTicketEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from WebSocketTicketEntity t where t.hash = :hash")
    Optional<WebSocketTicketEntity> findForUpdate(@Param("hash") String hash);

    @Modifying
    @Query("delete from WebSocketTicketEntity t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}

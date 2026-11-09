package at.control.shiftcontrol.shift.repo;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import at.shiftcontrol.lib.entity.TrustAlert;

public interface TrustAlertRepository extends JpaRepository<TrustAlert, Long> {
    @Query("""
            SELECT a
            FROM TrustAlert a
            ORDER BY a.createdAt DESC
            LIMIT :size OFFSET :offset
        """)
    Collection<TrustAlert> getAllPaginated(long offset, long size);

    @Query("""
            SELECT count(a)
            FROM TrustAlert a
        """)
    long findAllSize();
}

package at.conrunner.shift.dao;

import java.util.Collection;

import at.conrunner.shift.entity.TrustAlert;

public interface TrustAlertDao {
    Collection<TrustAlert> getAllPaginated(long page, long size);

    java.util.Optional<TrustAlert> findById(long id);

    TrustAlert save(TrustAlert alert);

    void delete(long id);

    long findAllSize();
}

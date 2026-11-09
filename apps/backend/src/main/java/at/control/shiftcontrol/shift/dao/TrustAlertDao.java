package at.control.shiftcontrol.shift.dao;

import java.util.Collection;

import at.control.shiftcontrol.shift.entity.TrustAlert;

public interface TrustAlertDao {
    Collection<TrustAlert> getAllPaginated(long page, long size);

    TrustAlert save(TrustAlert alert);

    void delete(long id);

    long findAllSize();
}

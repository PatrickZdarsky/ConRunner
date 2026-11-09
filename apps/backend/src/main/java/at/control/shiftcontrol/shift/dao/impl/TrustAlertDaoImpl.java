package at.control.shiftcontrol.shift.dao.impl;

import java.util.Collection;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.entity.TrustAlert;
import at.shiftcontrol.shiftservice.dao.TrustAlertDao;
import at.shiftcontrol.shiftservice.repo.TrustAlertRepository;

@RequiredArgsConstructor
@Component
public class TrustAlertDaoImpl implements TrustAlertDao {
    private final TrustAlertRepository repository;

    @Override
    public Collection<TrustAlert> getAllPaginated(long page, long size) {
        long offset = page * size;
        return repository.getAllPaginated(offset, size);
    }

    @Override
    public TrustAlert save(TrustAlert alert) {
        return repository.save(alert);
    }

    @Override
    public void delete(long id) {
        repository.deleteById(id);
    }

    @Override
    public long findAllSize() {
        return repository.findAllSize();
    }
}

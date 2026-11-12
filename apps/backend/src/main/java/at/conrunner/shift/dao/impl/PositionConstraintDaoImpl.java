package at.conrunner.shift.dao.impl;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.conrunner.shift.entity.PositionConstraint;
import at.conrunner.shift.entity.PositionConstraintId;
import at.conrunner.shift.dao.PositionConstraintDao;
import at.conrunner.shift.repo.PositionConstraintRepository;

@RequiredArgsConstructor
@Component
public class PositionConstraintDaoImpl implements PositionConstraintDao {
    private final PositionConstraintRepository positionConstraintRepository;

    @Override
    public @NonNull String getName() {
        return "PositionConstraint";
    }

    @Override
    public @NonNull Optional<PositionConstraint> findById(PositionConstraintId id) {
        return positionConstraintRepository.findById(id);
    }

    @Override
    public PositionConstraint save(PositionConstraint entity) {
        return positionConstraintRepository.save(entity);
    }

    @Override
    public Collection<PositionConstraint> saveAll(Collection<PositionConstraint> entities) {
        return positionConstraintRepository.saveAll(entities);
    }

    @Override
    public void delete(PositionConstraint entity) {
        positionConstraintRepository.delete(entity);
    }
}

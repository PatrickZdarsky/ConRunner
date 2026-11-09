package at.control.shiftcontrol.shift.dao.impl;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import at.shiftcontrol.lib.entity.RewardPointsTransaction;
import at.shiftcontrol.lib.exception.IllegalStateException;
import at.shiftcontrol.lib.exception.UnsupportedOperationException;
import at.shiftcontrol.shiftservice.dao.RewardPointsTransactionDao;
import at.shiftcontrol.shiftservice.dto.rewardpoints.EventPointsDto;
import at.shiftcontrol.shiftservice.mapper.RewardPointsMapper;
import at.shiftcontrol.shiftservice.repo.RewardPointTransactionRepository;

@RequiredArgsConstructor
@Component
@Slf4j
public class RewardPointsTransactionDaoImpl implements RewardPointsTransactionDao {
    @Override
    public @NonNull String getName() {
        return "RewardPointTransaction";
    }

    private final RewardPointTransactionRepository repo;

    @Override
    public @NonNull Optional<RewardPointsTransaction> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public RewardPointsTransaction save(RewardPointsTransaction entity) {
        findById(entity.getId())
            .ifPresent(e -> {
                throw new IllegalStateException("RewardPointTransaction already exists and cannot be updated.");
            });
        return repo.save(entity);
    }

    @Override
    public Collection<RewardPointsTransaction> saveAll(Collection<RewardPointsTransaction> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public void delete(RewardPointsTransaction entity) {
        // Ledger is append-only: deleting is not allowed by design.
        throw new UnsupportedOperationException("RewardPointTransaction is append-only and cannot be deleted", null);
    }

    @Override
    public long sumPointsByVolunteer(String volunteerId) {
        return repo.sumPointsByVolunteer(volunteerId);
    }

    @Override
    public long sumPointsByVolunteerAndEvent(String volunteerId, long eventId) {
        return repo.sumPointsByVolunteerAndEvent(volunteerId, eventId);
    }

    @Override
    public long sumPointsByVolunteerAndShiftPlan(String volunteerId, long shiftPlanId) {
        return repo.sumPointsByVolunteerAndShiftPlan(volunteerId, shiftPlanId);
    }

    @Override
    public Collection<EventPointsDto> sumPointsForUserGroupedByEvent(String volunteerId) {
        return RewardPointsMapper.toEventPointsDto(repo.sumPointsForUserGroupedByEvent(volunteerId));
    }

    public Collection<RewardPointsTransaction> findAllByVolunteerIdOrderByCreatedAtAsc(String volunteerId) {
        return repo.findAllByVolunteerIdOrderByCreatedAtAsc(volunteerId);
    }
}

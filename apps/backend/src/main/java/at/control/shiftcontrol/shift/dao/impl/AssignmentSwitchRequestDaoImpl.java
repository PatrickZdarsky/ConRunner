package at.control.shiftcontrol.shift.dao.impl;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import at.control.shiftcontrol.shift.entity.Assignment;
import at.control.shiftcontrol.shift.entity.AssignmentPair;
import at.control.shiftcontrol.shift.entity.AssignmentSwitchRequest;
import at.control.shiftcontrol.shift.lib.type.TradeStatus;
import at.control.shiftcontrol.shift.dao.AssignmentSwitchRequestDao;
import at.control.shiftcontrol.shift.repo.AssignmentSwitchRequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AssignmentSwitchRequestDaoImpl implements AssignmentSwitchRequestDao {
    private final AssignmentSwitchRequestRepository assignmentSwitchRequestRepository;
    private final at.control.shiftcontrol.shift.audit.OperationsAudit operationsAudit;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public @NonNull String getName() {
        return "AssignmentSwitchRequest";
    }

    @Override
    public @NonNull Optional<AssignmentSwitchRequest> findById(Long id) {
        return assignmentSwitchRequestRepository.findById(id);
    }

    @Override
    public Optional<AssignmentSwitchRequest> findBySlotsAndUsers(long offeredSlotId, UUID offeringUserId, long requestedSlotId, UUID requestedUserId) {
        return assignmentSwitchRequestRepository.findBySlotsAndUsers(offeredSlotId, offeringUserId, requestedSlotId, requestedUserId);
    }

    @Override
    public AssignmentSwitchRequest save(AssignmentSwitchRequest entity) {
        return assignmentSwitchRequestRepository.save(entity);
    }

    @Override
    public Collection<AssignmentSwitchRequest> saveAll(Collection<AssignmentSwitchRequest> entities) {
        return assignmentSwitchRequestRepository.saveAll(entities);
    }

    @Override
    public void delete(AssignmentSwitchRequest entity) {
        assignmentSwitchRequestRepository.delete(entity);
    }

    @Override
    public void cancelTradesForPositionSlot(Long positionSlotId, UUID assignedUser) {
        cancel(assignmentSwitchRequestRepository.lockTradesForAssignment(positionSlotId, assignedUser));
    }

    @Override
    public void cancelTradesForAssignment(Assignment assignment) {
        cancel(assignmentSwitchRequestRepository.lockTradesForAssignment(assignment.getPositionSlot().getId(), assignment.getAssignedVolunteer().getId()));
    }

    @Override
    public Collection<AssignmentSwitchRequest> findOpenTradesForRequestedPositionAndOfferingUser(long positionSlotId, UUID userId) {
        return assignmentSwitchRequestRepository.findOpenTradesForRequestedPositionAndOfferingUser(positionSlotId, userId, TradeStatus.OPEN);
    }

    @Override
    public Collection<AssignmentSwitchRequest> findTradesForShiftPlanAndUser(long shiftPlanId, UUID userId) {
        return assignmentSwitchRequestRepository.findTradesForShiftPlanAndUser(shiftPlanId, userId);
    }

    @Override
    public void cancelTradesForOfferedPositionAndRequestedUser(long positionSlotId, UUID userId) {
        cancel(assignmentSwitchRequestRepository.lockTradesForOfferedPositionAndRequestedUser(positionSlotId, userId));
    }

    private void cancel(Collection<AssignmentSwitchRequest> trades) {
        for (var trade : trades) {
            if (trade.getStatus() == TradeStatus.CANCELED) continue;
            var before = operationsAudit.snapshot(trade);
            trade.setStatus(TradeStatus.CANCELED);
            operationsAudit.trade(trade, "cancelled", before);
        }
    }

    @Override
    public List<AssignmentSwitchRequest> findInverseTrade(AssignmentSwitchRequest trade) {
        return assignmentSwitchRequestRepository.findByAssignmentIds(
            trade.getRequestedAssignment().getId(),
            trade.getOfferingAssignment().getId()
        );
    }

    public Collection<AssignmentSwitchRequest> findAllByAssignmentPairs(Collection<AssignmentPair> pairs) {
        if (pairs.isEmpty()) {
            return List.of();
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AssignmentSwitchRequest> cq = cb.createQuery(AssignmentSwitchRequest.class);
        Root<AssignmentSwitchRequest> root = cq.from(AssignmentSwitchRequest.class);

        List<Predicate> orPredicates = new ArrayList<>();

        for (AssignmentPair pair : pairs) {
            orPredicates.add(
                cb.and(
                    cb.equal(root.get("offeringAssignment").get("id"), pair.getOfferingId()),
                    cb.equal(root.get("requestedAssignment").get("id"), pair.getRequestedId())
                )
            );
        }

        cq.where(cb.or(orPredicates.toArray(new Predicate[0])));

        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public Collection<AssignmentSwitchRequest> findOpenTradesForOfferingPositionAndRequestedOwner(long positionSlotId, UUID userId) {
        return assignmentSwitchRequestRepository.findOpenTradesForOfferingPositionAndRequestedOwner(positionSlotId, userId, TradeStatus.OPEN);
    }
}

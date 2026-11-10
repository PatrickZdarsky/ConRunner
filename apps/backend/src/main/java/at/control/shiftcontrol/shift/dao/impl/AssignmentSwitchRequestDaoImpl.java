package at.control.shiftcontrol.shift.dao.impl;

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
    public Optional<AssignmentSwitchRequest> findBySlotsAndUsers(long offeredSlotId, String offeringUserId, long requestedSlotId, String requestedUserId) {
        return assignmentSwitchRequestRepository.findBySlotsAndUsers(offeredSlotId, java.util.UUID.fromString(offeringUserId), requestedSlotId, java.util.UUID.fromString(requestedUserId));
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
    public void cancelTradesForPositionSlot(Long positionSlotId, String assignedUser) {
        assignmentSwitchRequestRepository.cancelTradesForAssignment(positionSlotId, java.util.UUID.fromString(assignedUser), TradeStatus.CANCELED);
    }

    @Override
    public void cancelTradesForAssignment(Assignment assignment) {
        assignmentSwitchRequestRepository.cancelTradesForAssignment(assignment.getPositionSlot().getId(), assignment.getAssignedVolunteer().getId(),
            TradeStatus.CANCELED);
    }

    @Override
    public Collection<AssignmentSwitchRequest> findOpenTradesForRequestedPositionAndOfferingUser(long positionSlotId, String userId) {
        return assignmentSwitchRequestRepository.findOpenTradesForRequestedPositionAndOfferingUser(positionSlotId, java.util.UUID.fromString(userId), TradeStatus.OPEN);
    }

    @Override
    public Collection<AssignmentSwitchRequest> findTradesForShiftPlanAndUser(long shiftPlanId, String userId) {
        return assignmentSwitchRequestRepository.findTradesForShiftPlanAndUser(shiftPlanId, java.util.UUID.fromString(userId));
    }

    @Override
    public void cancelTradesForOfferedPositionAndRequestedUser(long positionSlotId, String userId) {
        assignmentSwitchRequestRepository.cancelTradesForOfferedPositionAndRequestedUser(positionSlotId, java.util.UUID.fromString(userId), TradeStatus.CANCELED);
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
    public Collection<AssignmentSwitchRequest> findOpenTradesForOfferingPositionAndRequestedOwner(long positionSlotId, String userId) {
        return assignmentSwitchRequestRepository.findOpenTradesForOfferingPositionAndRequestedOwner(positionSlotId, java.util.UUID.fromString(userId), TradeStatus.OPEN);
    }
}

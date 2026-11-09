package at.control.shiftcontrol.shift.dao.impl;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.entity.ShiftPlanInvite;
import at.shiftcontrol.lib.exception.NotFoundException;
import at.shiftcontrol.shiftservice.dao.ShiftPlanInviteDao;
import at.shiftcontrol.shiftservice.repo.ShiftPlanInviteRepository;

@RequiredArgsConstructor
@Component
public class ShiftPlanInviteDaoImpl implements ShiftPlanInviteDao {
    private final ShiftPlanInviteRepository shiftPlanInviteRepository;

    @Override
    public @NonNull String getName() {
        return "ShiftPlanInvite";
    }

    @Override
    public @NonNull Optional<ShiftPlanInvite> findById(Long id) {
        return shiftPlanInviteRepository.findById(id);
    }

    @Override
    public ShiftPlanInvite save(ShiftPlanInvite entity) {
        return shiftPlanInviteRepository.save(entity);
    }

    @Override
    public Collection<ShiftPlanInvite> saveAll(Collection<ShiftPlanInvite> entities) {
        return shiftPlanInviteRepository.saveAll(entities);
    }

    @Override
    public void delete(ShiftPlanInvite entity) {
        shiftPlanInviteRepository.delete(entity);
    }

    @Override
    public ShiftPlanInvite getByCode(String code) {
        return shiftPlanInviteRepository.findByCode(code).orElseThrow(() -> new NotFoundException("Invite code not found"));
    }

    @Override
    public boolean existsByCode(String code) {
        return shiftPlanInviteRepository.existsByCode(code);
    }

    @Override
    public Collection<ShiftPlanInvite> findAllByShiftPlanId(Long shiftPlanId) {
        return shiftPlanInviteRepository.findAllByShiftPlanId(shiftPlanId);
    }
}

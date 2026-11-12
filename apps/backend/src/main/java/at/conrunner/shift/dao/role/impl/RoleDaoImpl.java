package at.conrunner.shift.dao.role.impl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.conrunner.shift.entity.Role;
import at.conrunner.platform.exception.PartiallyNotFoundException;
import at.conrunner.shift.dao.role.RoleDao;
import at.conrunner.shift.repo.role.RoleRepository;

@RequiredArgsConstructor
@Component
public class RoleDaoImpl implements RoleDao {
    private final RoleRepository roleRepository;

    @Override
    public @NonNull String getName() {
        return "Role";
    }

    @Override
    public @NonNull Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    @Override
    public Collection<Role> findAllById(Collection<Long> roleIds) {
        return roleRepository.findAllById(roleIds);
    }

    @Override
    public Role save(Role entity) {
        return roleRepository.save(entity);
    }

    @Override
    public Collection<Role> saveAll(Collection<Role> entities) {
        return roleRepository.saveAll(entities);
    }

    @Override
    public void delete(Role entity) {
        roleRepository.delete(entity);
    }

    @Override
    public List<Role> findAllByShiftPlanId(Long shiftPlanId) {
        return roleRepository.findAllByShiftPlanId(shiftPlanId);
    }

    @Override
    public Collection<Role> getRolesByIdsAndShiftPlanId(Set<Long> roleIds, Long shiftPlanId) {
        var roles = roleRepository.getRolesByIdsAndShiftPlanId(roleIds, shiftPlanId);
        if (roles.size() != roleIds.size()) {
            var foundId = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toSet());
            roleIds.removeAll(foundId);
            throw PartiallyNotFoundException.of(getName(), roleIds);
        }
        return roles;
    }

    @Override
    public Collection<Role> getByIds(Set<Long> roleIds) {
        return roleRepository.getByIds(roleIds);
    }

    @Override
    public Optional<Role> findByNameAndShiftPlanId(String name, long shiftPlanId) {
        return roleRepository.findByNameAndShiftPlanId(name, shiftPlanId);
    }
}

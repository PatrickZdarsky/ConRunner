package at.control.shiftcontrol.shift.dao.impl;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.shiftcontrol.lib.entity.PositionSlot;
import at.shiftcontrol.lib.entity.PositionSlotPreference;
import at.shiftcontrol.lib.entity.PositionSlotPreferenceId;
import at.shiftcontrol.shiftservice.dao.PositionSlotDao;
import at.shiftcontrol.shiftservice.repo.PositionSlotPreferenceRepository;
import at.shiftcontrol.shiftservice.repo.PositionSlotRepository;

@RequiredArgsConstructor
@Component
public class PositionSlotDaoImpl implements PositionSlotDao {
    private final PositionSlotRepository positionSlotRepository;
    private final PositionSlotPreferenceRepository positionSlotPreferenceRepository;

    @Override
    public @NonNull String getName() {
        return "PositionSlot";
    }

    @Override
    public @NonNull Optional<PositionSlot> findById(Long id) {
        return positionSlotRepository.findById(id);
    }

    @Override
    public PositionSlot save(PositionSlot entity) {
        return positionSlotRepository.save(entity);
    }

    @Override
    public Collection<PositionSlot> saveAll(Collection<PositionSlot> entities) {
        return positionSlotRepository.saveAll(entities);
    }

    @Override
    public void delete(PositionSlot entity) {
        positionSlotRepository.delete(entity);
    }

    @Override
    public void setPreference(@NonNull String volunteerId, long positionSlotId, int preference) {
        var preferenceId = PositionSlotPreferenceId.of(volunteerId, positionSlotId);
        var positionSlotPreference = positionSlotPreferenceRepository.findById(preferenceId);
        if (positionSlotPreference.isPresent()) {
            var existingPreference = positionSlotPreference.get();
            existingPreference.setPreferenceLevel(preference);
            positionSlotPreferenceRepository.save(existingPreference);
        } else {
            var newPreference = PositionSlotPreference.builder()
                    .id(preferenceId)
                    .preferenceLevel(preference)
                    .build();
            positionSlotPreferenceRepository.save(newPreference);
        }
    }

    @Override
    public int getPreference(@NonNull String volunteerId, long positionSlotId) {
        var preferenceId = PositionSlotPreferenceId.of(volunteerId, positionSlotId);
        var positionSlotPreference = positionSlotPreferenceRepository.findById(preferenceId);

        return positionSlotPreference.map(PositionSlotPreference::getPreferenceLevel).orElse(0);
    }
}

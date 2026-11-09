package at.control.shiftcontrol.shift.dao.impl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

import at.control.shiftcontrol.shift.entity.Location;
import at.control.shiftcontrol.shift.dao.LocationDao;
import at.control.shiftcontrol.shift.dto.location.LocationSearchDto;
import at.control.shiftcontrol.shift.repo.LocationRepository;

@RequiredArgsConstructor
@Component
public class LocationDaoImpl implements LocationDao {
    private final LocationRepository locationRepository;

    @Override
    public @NonNull String getName() {
        return "Location";
    }

    @Override
    public @NonNull Optional<Location> findById(Long id) {
        return locationRepository.findById(id);
    }

    @Override
    public Location save(Location entity) {
        return locationRepository.save(entity);
    }

    @Override
    public Collection<Location> saveAll(Collection<Location> entities) {
        return locationRepository.saveAll(entities);
    }

    @Override
    public void delete(Location entity) {
        locationRepository.delete(entity);
    }

    @Override
    public Collection<Location> findAllByEventId(Long eventId) {
        return locationRepository.findAllByEventId(eventId);
    }

    @Override
    public List<Location> search(long eventId, LocationSearchDto searchDto) {
        return locationRepository.findAll((Specification<Location>) (root, query, criteriaBuilder) -> {
            if (searchDto == null || StringUtils.isBlank(searchDto.getName())) {
                return null; //Select all
            }
            var predicates = criteriaBuilder.equal(root.get("event").get("id"), eventId);

            if (searchDto.getName() != null && !searchDto.getName().isEmpty()) {
                var nameLower = searchDto.getName().toLowerCase().trim();
                predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + nameLower + "%"));
            }

            return predicates;
        });
    }

    @Override
    public Optional<Location> findByEventAndName(long eventId, String name) {
        return locationRepository.findByEventAndName(eventId, name);
    }
}

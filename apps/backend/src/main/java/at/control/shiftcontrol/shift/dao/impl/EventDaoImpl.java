package at.control.shiftcontrol.shift.dao.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.dao.EventDao;
import at.control.shiftcontrol.shift.dto.event.EventSearchDto;
import at.control.shiftcontrol.shift.dto.rows.PlanVolunteerIdRow;
import at.control.shiftcontrol.shift.repo.EventRepository;

@RequiredArgsConstructor
@Component
public class EventDaoImpl implements EventDao {
    private final EventRepository eventRepository;

    @Override
    public @NonNull String getName() {
        return "Event";
    }

    @Override
    public @NonNull Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }

    @Override
    public Event save(Event entity) {
        return eventRepository.save(entity);
    }

    @Override
    public Collection<Event> saveAll(Collection<Event> entities) {
        return eventRepository.saveAll(entities);
    }

    @Override
    public void delete(Event entity) {
        eventRepository.delete(entity);
    }

    @Override
    public List<Event> search(EventSearchDto searchDto) {
        return eventRepository.findAll((Specification<Event>) (root, query, criteriaBuilder) -> {
            if (searchDto == null || StringUtils.isBlank(searchDto.getName())) {
                return null; //Select all
            }
            var predicates = criteriaBuilder.conjunction();

            if (searchDto.getName() != null && !searchDto.getName().isEmpty()) {
                var nameLower = searchDto.getName().toLowerCase().trim();
                predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + nameLower + "%"));
            }

            return predicates;
        });
    }

    @Override
    public Collection<Event> getAllOpenEvents() {
        return eventRepository.getAllOpenEvents(Instant.now());
    }

    @Override
    public Collection<Event> getAllOpenEventsForUser(String userId) {
        return eventRepository.getAllOpenEventsForUser(java.util.UUID.fromString(userId), Instant.now());
    }

    @Override
    public Collection<Event> findAll() {
        return eventRepository.findAll();
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return eventRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public Collection<PlanVolunteerIdRow> getPlannersForEventAndUser(long eventId, String userId) {
        return eventRepository.getPlannersForEventAndUser(eventId, java.util.UUID.fromString(userId));
    }

    @Override
    public Optional<Event> findByName(String name) {
        return eventRepository.findByName(name);
    }
}

package at.control.shiftcontrol.shift.dao.impl;

import at.control.shiftcontrol.identity.User;
import at.control.shiftcontrol.shift.dao.userprofile.impl.VolunteerDaoImpl;
import at.control.shiftcontrol.shift.entity.Volunteer;
import at.control.shiftcontrol.shift.mapper.UserAssemblingMapper;
import at.control.shiftcontrol.shift.repo.AssignmentRepository;
import at.control.shiftcontrol.shift.repo.VolunteerRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VolunteerUuidTest {
    @Test
    void volunteerLookupAndUserAssemblyUseTheSameUuid() {
        var id = UUID.randomUUID();
        var volunteer = Volunteer.builder().id(id).planningPlans(List.of()).volunteeringPlans(List.of())
            .lockedPlans(List.of()).roles(List.of()).build();
        var repository = mock(VolunteerRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(volunteer));
        assertThat(new VolunteerDaoImpl(repository).findById(id)).contains(volunteer);

        var user = User.builder().id(id).username("Ada").email("ada@example.org").build();
        var result = UserAssemblingMapper.toUserEventDtoForUsers(List.of(volunteer), List.of(user));
        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getVolunteer().getId()).isEqualTo(id);
            assertThat(dto.getVolunteer().getName()).isEqualTo("Ada");
            assertThat(dto.getVolunteeringPlans()).isEmpty();
        });
    }

    @Test
    void assignmentLookupPassesUuidToTheRepository() {
        var repository = mock(AssignmentRepository.class);
        var id = UUID.randomUUID();
        new AssignmentDaoImpl(repository).findBySlotAndUser(42L, id);
        verify(repository).findBySlotAndUser(42L, id);
    }
}

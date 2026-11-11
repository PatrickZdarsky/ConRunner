package at.control.shiftcontrol.shift.service.impl;

import at.control.shiftcontrol.shift.dao.EventDao;
import at.control.shiftcontrol.shift.dao.ShiftPlanDao;
import at.control.shiftcontrol.shift.dao.ShiftPlanInviteDao;
import at.control.shiftcontrol.shift.dto.shiftplan.ShiftPlanModificationDto;
import at.control.shiftcontrol.shift.entity.Event;
import at.control.shiftcontrol.shift.entity.ShiftPlan;
import at.control.shiftcontrol.shift.entity.ShiftPlanInvite;
import at.control.shiftcontrol.shift.lib.type.LockStatus;
import at.control.shiftcontrol.shift.lib.type.ShiftPlanInviteType;
import at.control.shiftcontrol.shift.util.UniqueCodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftPlanServiceImplTest {
    @Mock at.control.shiftcontrol.shift.audit.OperationsAudit operationsAudit;
    @Mock at.control.shiftcontrol.shift.audit.PlanningAudit planningAudit;
    @Mock EventDao eventDao;
    @Mock ShiftPlanDao shiftPlanDao;
    @Mock ShiftPlanInviteDao inviteDao;
    @Mock UniqueCodeGenerator codeGenerator;
    @Mock ApplicationEventPublisher publisher;
    @InjectMocks ShiftPlanServiceImpl service;

    @Test
    void creatingPlanCreatesDefaultVolunteerAndPlannerInvites() {
        var event = Event.builder().id(7L).build();
        when(eventDao.getById(7L)).thenReturn(event);
        when(shiftPlanDao.findByEventId(7L)).thenReturn(List.of());
        when(shiftPlanDao.save(any(ShiftPlan.class))).thenAnswer(invocation -> {
            ShiftPlan plan = invocation.getArgument(0);
            plan.setId(9L);
            return plan;
        });
        when(codeGenerator.generateUnique(anyString(), anyInt(), anyInt(), any())).thenReturn("VOLCODE2", "PLANCODE");
        when(inviteDao.save(any(ShiftPlanInvite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createShiftPlan(7L, ShiftPlanModificationDto.builder().name("Crew").build());

        assertEquals("9", result.getShiftPlan().getId());
        assertEquals(LockStatus.SELF_SIGNUP, result.getShiftPlan().getLockStatus());
        assertEquals(ShiftPlanInviteType.VOLUNTEER_JOIN, result.getVolunteerInvite().getType());
        assertEquals("VOLCODE2", result.getVolunteerInvite().getCode());
        assertEquals(ShiftPlanInviteType.PLANNER_JOIN, result.getPlannerInvite().getType());
        assertEquals("PLANCODE", result.getPlannerInvite().getCode());
        verify(inviteDao, times(2)).save(any(ShiftPlanInvite.class));
        verify(publisher).publishEvent(any(Object.class));
    }
}

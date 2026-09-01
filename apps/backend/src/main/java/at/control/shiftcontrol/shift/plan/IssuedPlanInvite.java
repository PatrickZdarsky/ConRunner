package at.control.shiftcontrol.shift.plan;

import java.util.UUID;

/** The raw code is returned only from the authorized issuance command. */
public record IssuedPlanInvite(UUID inviteId, String code) {
}

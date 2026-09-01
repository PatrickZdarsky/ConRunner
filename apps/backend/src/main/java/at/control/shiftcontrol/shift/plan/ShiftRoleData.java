package at.control.shiftcontrol.shift.plan;

public record ShiftRoleData(String name, String description, boolean selfAssignable) {
    public ShiftRoleData {
        name = ShiftPlanData.required(name, "Role name");
        description = ShiftPlanData.optional(description);
    }
}

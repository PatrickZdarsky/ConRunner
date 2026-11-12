package at.conrunner.identity;

public final class Authorities {
    public static final String PLATFORM_ADMIN_ROLE = "admin";
    static final String HAS_PLATFORM_ADMIN =
        "!authentication.authorities.?[authority.equalsIgnoreCase('" + PLATFORM_ADMIN_ROLE + "')].isEmpty()";
    public static final String LEGACY_PLATFORM_ADMIN_SCOPE = "shiftcontrol.admin";
    public static final String INTERNAL_USERS_READ = "shiftservice.users.read";
    public static final String CAN_JOIN_UNELIGIBLE_POSITIONS = "shiftservice.positions.can_join_uneligible_shifts";
    public static final String CAN_LEAVE_LOCKED_POSITIONS = "shiftservice.positions.can_leave_locked_positions";
}

package at.control.shiftcontrol.shift.userdirectory.current;

public record CurrentSubjectProfile(
    String issuer,
    String subject,
    String preferredUsername,
    String firstName,
    String lastName,
    String email,
    String profile,
    Boolean emailVerified,
    boolean isPlatformAdmin
) {}

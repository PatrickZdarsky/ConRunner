package at.control.shiftcontrol.shift.error;

public class ShiftAccessDeniedException extends RuntimeException {
    public ShiftAccessDeniedException(String message) {
        super(message);
    }
}

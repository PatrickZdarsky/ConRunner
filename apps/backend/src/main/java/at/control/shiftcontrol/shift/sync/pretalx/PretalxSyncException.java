package at.control.shiftcontrol.shift.sync.pretalx;

public class PretalxSyncException extends RuntimeException {
    public PretalxSyncException(String message) {
        super(message);
    }

    public PretalxSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}

package at.control.shiftcontrol.shift.sync.pretalx;

public class PretalxMissingApiKeyException extends RuntimeException {
    public PretalxMissingApiKeyException(String message) {
        super(message);
    }

    public static PretalxMissingApiKeyException forEventSlug(String eventSlug) {
        return new PretalxMissingApiKeyException("No API key found for Pretalx event slug: " + eventSlug);
    }
}

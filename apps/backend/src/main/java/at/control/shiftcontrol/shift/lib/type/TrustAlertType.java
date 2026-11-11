package at.control.shiftcontrol.shift.lib.type;

public enum TrustAlertType {

    SPAM("The volunteer signed up and off at least 3 times for the same slot within 1 hour."),
    OVERLOAD("The volunteer signed up for at least 5 different slots within 30 minutes."),
    TRADE("The volunteer has 10 or more open trades."),
    AUCTION("The volunteer has 5 or more open auctions.");

    private final String description;

    TrustAlertType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

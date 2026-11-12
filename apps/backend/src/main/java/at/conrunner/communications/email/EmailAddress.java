package at.conrunner.communications.email;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

public final class EmailAddress {
    private EmailAddress() {}

    public static boolean isValid(String value) {
        if (value == null || value.isBlank() || value.length() > 320 || !value.equals(value.trim())
            || value.chars().anyMatch(Character::isISOControl)) return false;
        try {
            var address = new InternetAddress(value, true);
            address.validate();
            return value.equals(address.getAddress()) && address.getPersonal() == null;
        } catch (AddressException e) {
            return false;
        }
    }
}

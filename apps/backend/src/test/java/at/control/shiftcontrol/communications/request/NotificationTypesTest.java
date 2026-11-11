package at.control.shiftcontrol.communications.request;

import at.control.shiftcontrol.communications.api.NotificationTypes;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationTypesTest {
    @Test
    void germanBundleCoversEveryEnglishMessage() {
        var control = java.util.ResourceBundle.Control.getNoFallbackControl(java.util.ResourceBundle.Control.FORMAT_PROPERTIES);
        var english = java.util.ResourceBundle.getBundle("notification-messages", java.util.Locale.ROOT, control);
        var german = java.util.ResourceBundle.getBundle("notification-messages", java.util.Locale.GERMAN, control);
        var rootKeys = english.keySet();
        var germanFile = new java.util.Properties();
        try (var stream = getClass().getResourceAsStream("/notification-messages_de.properties")) {
            germanFile.load(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        assertThat(germanFile.stringPropertyNames()).containsExactlyInAnyOrderElementsOf(rootKeys);
        assertThat(NotificationTemplates.get(NotificationTypes.TRUST_ALERT, java.util.Locale.forLanguageTag("fr")).title())
            .isEqualTo("Trust alert");
        assertThat(german.getString("shift.trust.alert.title")).isEqualTo("Vertrauenshinweis");
    }

    @Test
    void furryConventionBundleCoversEveryMessageAndRendersContext() throws java.io.IOException {
        var english = new java.util.Properties();
        var furry = new java.util.Properties();
        try (var en = getClass().getResourceAsStream("/notification-messages.properties");
             var uwu = getClass().getResourceAsStream("/notification-messages-uwu.properties")) {
            english.load(new java.io.InputStreamReader(en, java.nio.charset.StandardCharsets.UTF_8));
            furry.load(new java.io.InputStreamReader(uwu, java.nio.charset.StandardCharsets.UTF_8));
        }
        assertThat(furry.stringPropertyNames()).containsExactlyInAnyOrderElementsOf(english.stringPropertyNames());
        var message = NotificationTemplates.render(NotificationTypes.ROLE_ASSIGNED,
            new at.control.shiftcontrol.communications.api.NotificationContext("42", "Registration", ""),
            java.util.Locale.forLanguageTag("en-x-uwu"));
        assertThat(message.title()).isEqualTo("New role 🐾");
        assertThat(message.body()).isEqualTo("You were assigned the role 'Registration'. Paws up!");
    }
    @Test
    void everyPublicTypeHasExactlyOneTemplate() throws IllegalAccessException {
        var fields = Arrays.stream(NotificationTypes.class.getDeclaredFields())
            .filter(field -> Modifier.isPublic(field.getModifiers()) && field.getType() == String.class).toList();
        var values = new java.util.ArrayList<String>();
        for (var field : fields) values.add((String) field.get(null));
        assertThat(values).doesNotHaveDuplicates();
        assertThat(NotificationTemplates.types()).containsExactlyInAnyOrderElementsOf(values);
    }
}

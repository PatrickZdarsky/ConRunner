package at.control.shiftcontrol.communications.preference;

import at.control.shiftcontrol.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import java.util.Locale;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import({TestcontainersConfiguration.class, NotificationLanguageService.class})
class NotificationLanguagePersistenceTest {
    @Autowired NotificationLanguageService languages;

    @Test
    void storesLanguagePerRecipientAndDefaultsToEnglish() {
        var user = UUID.randomUUID();
        var other = UUID.randomUUID();
        assertThat(languages.languageFor(user)).isEqualTo(Locale.ENGLISH);
        languages.setLanguage(user, "de");
        assertThat(languages.languageFor(user)).isEqualTo(Locale.GERMAN);
        assertThat(languages.languageFor(other)).isEqualTo(Locale.ENGLISH);
        languages.setLanguage(user, "en-x-uwu");
        assertThat(languages.languageFor(user).toLanguageTag()).isEqualTo("en-x-uwu");
        languages.setLanguage(user, "en");
        assertThat(languages.languageFor(user)).isEqualTo(Locale.ENGLISH);
        assertThatIllegalArgumentException().isThrownBy(() -> languages.setLanguage(user, "fr"));
    }
}

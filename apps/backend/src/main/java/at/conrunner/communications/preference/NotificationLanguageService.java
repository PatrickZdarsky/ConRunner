package at.conrunner.communications.preference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationLanguageService {
    private static final Set<String> SUPPORTED = Set.of("en", "de", "en-x-uwu");
    private final NotificationLanguageRepository repository;
    NotificationLanguageService(NotificationLanguageRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public Locale languageFor(UUID recipientId) {
        return repository.findById(recipientId).map(p -> Locale.forLanguageTag(p.language)).orElse(Locale.ENGLISH);
    }

    @Transactional
    public void setLanguage(UUID recipientId, String language) {
        if (language == null || !SUPPORTED.contains(language)) throw new IllegalArgumentException("Unsupported notification language");
        repository.setLanguage(recipientId, language);
    }
}

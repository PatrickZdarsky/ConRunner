package at.control.shiftcontrol.shift.util;

import java.security.SecureRandom;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import at.shiftcontrol.lib.exception.IllegalStateException;

@Component
@Slf4j
public class UniqueCodeGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateUnique(
        String alphabet,
        int length,
        int maxAttempts,
        Predicate<String> existsInStore
    ) {
        char[] chars = alphabet.toCharArray();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String code = randomString(chars, length);
            if (!existsInStore.test(code)) {
                return code;
            }
        }

        log.error("Failed to generate unique code after {} attempts", maxAttempts);
        throw new IllegalStateException("Something went wrong while generating the unique code");
    }

    private String randomString(char[] alphabet, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = secureRandom.nextInt(alphabet.length);
            sb.append(alphabet[idx]);
        }
        return sb.toString();
    }
}

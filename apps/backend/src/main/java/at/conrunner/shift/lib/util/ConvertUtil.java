package at.conrunner.shift.lib.util;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import at.conrunner.platform.exception.BadRequestException;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ConvertUtil {
    public static long idToLong(String value) throws BadRequestException {
        if (value == null || value.isEmpty()) {
            log.error("Id is not present.");
            throw new BadRequestException("Something unexpected happened while processing the request.");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException _) {
            log.error("Id is not valid: {}", value);
            // return user friendly message without mentioning exception details like id
            throw new BadRequestException("Something unexpected happened while processing the request.");
        }
    }

    public static Collection<Long> idToLong(Collection<String> value) throws BadRequestException {
        return value.stream().map(ConvertUtil::idToLong).toList();
    }

    public static List<String> toStringList(List<Long> ids) {
        return toStringList(ids.stream());
    }

    public static List<String> toStringList(Stream<Long> stream) {
        return stream.map(String::valueOf).toList();
    }
}

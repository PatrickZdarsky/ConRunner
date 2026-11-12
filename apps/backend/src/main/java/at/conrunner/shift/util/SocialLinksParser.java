package at.conrunner.shift.util;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import at.conrunner.platform.exception.IllegalArgumentException;
import at.conrunner.shift.lib.type.SocialMediaLinkType;
import at.conrunner.shift.dto.event.SocialMediaLinkDto;

public class SocialLinksParser {
    private SocialLinksParser() {
    }

    private static final Pattern SPLIT = Pattern.compile("\\s*,\\s*");

    private static final Map<SocialMediaLinkType, Set<String>> DOMAIN_LABELS = Map.of(
        SocialMediaLinkType.INSTAGRAM, Set.of("instagram"),
        SocialMediaLinkType.FACEBOOK, Set.of("facebook"),
        SocialMediaLinkType.X, Set.of("x", "twitter"),
        SocialMediaLinkType.TIKTOK, Set.of("tiktok"),
        SocialMediaLinkType.YOUTUBE, Set.of("youtube", "youtu"),
        SocialMediaLinkType.LINKEDIN, Set.of("linkedin"),
        SocialMediaLinkType.TWITCH, Set.of("twitch"),
        SocialMediaLinkType.DISCORD, Set.of("discord"),
        SocialMediaLinkType.REDDIT, Set.of("reddit")
    );

    public static List<SocialMediaLinkDto> parseToDtos(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }

        var parsed = new ArrayList<SocialMediaLinkDto>();
        for (String token : SPLIT.split(csv.trim())) {
            if (token == null || token.isBlank()) {
                continue;
            }

            String normalizedUrl = normalizeUrl(token);
            SocialMediaLinkType type = inferType(normalizedUrl);

            parsed.add(SocialMediaLinkDto.builder()
                .type(type)
                .url(normalizedUrl)
                .build());
        }

        return dedupeByKey(parsed);
    }

    private static List<SocialMediaLinkDto> dedupeByKey(List<SocialMediaLinkDto> input) {
        var seen = new HashSet<String>();
        var out = new ArrayList<SocialMediaLinkDto>();

        for (SocialMediaLinkDto dto : input) {
            String key = dto.createKey();
            if (seen.add(key)) {
                out.add(dto);
            }
        }

        return out;
    }

    private static String normalizeUrl(String input) {
        String trimmed = input.trim();

        // Allow users to paste "instagram.com/xyz" without scheme
        if (!trimmed.contains("://")) {
            trimmed = "https://" + trimmed;
        }

        URI uri = safeParse(trimmed);

        // normalize host to lowercase + ASCII (punycode), remove trailing slash
        String scheme = (uri.getScheme() == null) ? "https" : uri.getScheme().toLowerCase();
        String host = uri.getHost();
        if (host != null) {
            host = IDN.toASCII(host.toLowerCase());
        }

        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        String query = uri.getRawQuery() == null ? null : uri.getRawQuery();

        // rebuild to normalized form
        URI normalized = safeParse(buildUriString(scheme, host, path, query));

        String s = normalized.toString();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static String buildUriString(String scheme, String host, String path, String query) {
        StringBuilder b = new StringBuilder();
        b.append(scheme).append("://");
        if (host != null) {
            b.append(host);
        }
        if (path != null && !path.isBlank()) {
            if (!path.startsWith("/")) {
                b.append("/");
            }
            b.append(path);
        }
        if (query != null && !query.isBlank()) {
            b.append("?").append(query);
        }
        return b.toString();
    }

    private static URI safeParse(String s) {
        try {
            return new URI(s);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + s);
        }
    }

    private static SocialMediaLinkType inferType(String normalizedUrl) {
        URI uri = safeParse(normalizedUrl);
        String host = uri.getHost();

        if (host == null) {
            return SocialMediaLinkType.OTHER;
        }

        String base = extractBaseLabel(host);
        if (base == null) {
            return SocialMediaLinkType.OTHER;
        }

        for (var entry : DOMAIN_LABELS.entrySet()) {
            if (entry.getValue().contains(base)) {
                return entry.getKey();
            }
        }

        return SocialMediaLinkType.WEBSITE;
    }


    private static String extractBaseLabel(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }

        host = host.toLowerCase();

        if (host.startsWith("www.")) {
            host = host.substring(4);
        }

        String[] parts = host.split("\\.");
        if (parts.length < 2) {
            return null;
        }

        // Handle multi-part TLDs like co.uk, com.au, etc.
        int index = parts.length - 2;
        return parts[index];
    }
}

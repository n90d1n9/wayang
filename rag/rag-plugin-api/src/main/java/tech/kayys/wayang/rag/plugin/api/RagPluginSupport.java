package tech.kayys.wayang.rag.plugin.api;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Shared helpers for small, deterministic RAG plugin transformations.
 */
public final class RagPluginSupport {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9]+");

    private RagPluginSupport() {
    }

    public static String normalizeQuery(String query, boolean lowercase, int maxLength) {
        String normalized = normalizeWhitespace(query);
        if (lowercase) {
            normalized = normalized.toLowerCase(Locale.ROOT);
        }
        if (maxLength > 0 && normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }
        return normalized;
    }

    public static String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return WHITESPACE.matcher(text.trim()).replaceAll(" ");
    }

    public static Set<String> lowercaseCsvTerms(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(token -> token.toLowerCase(Locale.ROOT))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<String> tokenizeLowercase(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return TOKEN_SPLIT.splitAsStream(text.toLowerCase(Locale.ROOT))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean containsAnyTermIgnoreCase(String text, Set<String> lowercaseTerms) {
        if (text == null || text.isEmpty() || lowercaseTerms == null || lowercaseTerms.isEmpty()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return lowercaseTerms.stream().anyMatch(normalized::contains);
    }

    public static String redactTermsIgnoreCase(String text, Set<String> terms, String mask) {
        if (text == null || text.isEmpty() || terms == null || terms.isEmpty()) {
            return text;
        }
        String effectiveMask = (mask == null || mask.isBlank()) ? "[REDACTED]" : mask;
        String redacted = text;
        for (String term : terms) {
            if (term != null && !term.isEmpty()) {
                redacted = redacted.replaceAll("(?i)" + Pattern.quote(term), effectiveMask);
            }
        }
        return redacted;
    }

    public static double nonNegativeOr(double value, double fallback) {
        return Double.isFinite(value) && value >= 0.0 ? value : fallback;
    }

    public static Map<String, Object> metadataWith(Map<String, Object> metadata, String key, Object value) {
        return RagPluginMetadata.with(metadata, key, value);
    }

    public static Map<String, Object> metadataWith(Map<String, Object> metadata, Map<String, Object> additions) {
        return RagPluginMetadata.with(metadata, additions);
    }
}

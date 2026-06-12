package tech.kayys.wayang.gollek.cli;

import java.util.List;
import java.util.Map;

/**
 * Small text normalization helpers shared by Wayang CLI option parsing and renderers.
 */
final class CliText {

    private static final String NL = System.lineSeparator();

    private CliText() {
    }

    static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    static String blankToNull(String value) {
        String normalized = trimToEmpty(value);
        return normalized.isEmpty() ? null : normalized;
    }

    static String trimToDefault(String value, String defaultValue) {
        String normalized = trimToEmpty(value);
        return normalized.isEmpty() ? defaultValue : normalized;
    }

    static String commaSeparated(List<String> values) {
        return String.join(", ", values == null ? List.of() : values);
    }

    static String inlineKeyValueMap(Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((first, next) -> first + ", " + next)
                .orElse("");
    }

    static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    static String yesNo(Object value) {
        return yesNo(Boolean.TRUE.equals(value));
    }

    static void appendCommaSeparatedToken(StringBuilder output, String value) {
        if (output.length() > 0) {
            output.append(", ");
        }
        output.append(value);
    }

    static void appendCommaSeparatedTokenIf(StringBuilder output, String value, boolean enabled) {
        if (enabled) {
            appendCommaSeparatedToken(output, value);
        }
    }

    static void appendListLine(StringBuilder output, String label, List<String> values) {
        appendListLine(output, "", label, values);
    }

    static void appendIndentedListLine(StringBuilder output, String label, List<String> values) {
        appendListLine(output, "    ", label, values);
    }

    static void appendBulletBlock(StringBuilder output, String label, Iterable<?> values) {
        output.append(label).append(":").append(NL);
        if (!appendBulletItems(output, values)) {
            output.append("- none").append(NL);
        }
    }

    static void appendBulletBlockIfAny(StringBuilder output, String label, Iterable<?> values) {
        StringBuilder block = new StringBuilder(label).append(":").append(NL);
        if (appendBulletItems(block, values)) {
            output.append(block);
        }
    }

    private static void appendListLine(
            StringBuilder output,
            String prefix,
            String label,
            List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        output.append(prefix)
                .append(label)
                .append(": ")
                .append(commaSeparated(values))
                .append('\n');
    }

    private static boolean appendBulletItems(StringBuilder output, Iterable<?> values) {
        boolean found = false;
        if (values == null) {
            return false;
        }
        for (Object value : values) {
            output.append("- ").append(value).append(NL);
            found = true;
        }
        return found;
    }
}

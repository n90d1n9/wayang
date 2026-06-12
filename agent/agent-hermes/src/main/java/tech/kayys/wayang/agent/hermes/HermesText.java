package tech.kayys.wayang.agent.hermes;

import java.util.List;

final class HermesText {

    private HermesText() {
    }

    static String oneLine(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    static String oneLineOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : oneLine(value);
    }

    static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    static String trimOr(String value, String fallback) {
        String trimmed = trimToEmpty(value);
        return trimmed.isBlank() ? fallback : trimmed;
    }

    static List<String> trimmedList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList());
    }

    static List<String> distinctTrimmedList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
    }

    static List<String> distinctOneLineList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values.stream()
                .map(value -> oneLineOr(value, ""))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
    }
}

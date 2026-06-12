package tech.kayys.wayang.agenticcommerce.wayang;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Byte-size parsing helpers for deployable Wayang configuration.
 */
final class AgenticCommerceWayangByteSizes {

    static final String ISSUE_INVALID_BYTE_SIZE = "invalid_byte_size";

    private AgenticCommerceWayangByteSizes() {
    }

    static long parse(Object value, long defaultValue) {
        return parse(value).orElse(defaultValue);
    }

    static OptionalLong parse(Object value) {
        if (value instanceof Number number) {
            return OptionalLong.of(Math.max(0L, number.longValue()));
        }
        String normalized = AgenticCommerceWayangMaps.text(value)
                .replace("_", "")
                .replace(",", "")
                .replace(" ", "")
                .toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return OptionalLong.empty();
        }
        if ("unlimited".equals(normalized)
                || "none".equals(normalized)
                || "off".equals(normalized)
                || "disabled".equals(normalized)) {
            return OptionalLong.of(0L);
        }
        int suffixIndex = suffixIndex(normalized);
        String amount = suffixIndex < 0 ? normalized : normalized.substring(0, suffixIndex);
        String unit = suffixIndex < 0 ? "" : normalized.substring(suffixIndex);
        if (amount.isBlank()) {
            return OptionalLong.empty();
        }
        try {
            BigDecimal bytes = new BigDecimal(amount)
                    .multiply(BigDecimal.valueOf(multiplier(unit)));
            if (bytes.signum() < 0) {
                return OptionalLong.of(0L);
            }
            if (bytes.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
                return OptionalLong.of(Long.MAX_VALUE);
            }
            return OptionalLong.of(bytes.setScale(0, RoundingMode.CEILING).longValue());
        } catch (NumberFormatException exception) {
            return OptionalLong.empty();
        } catch (IllegalArgumentException exception) {
            return OptionalLong.empty();
        }
    }

    static ParseReport parseReport(Object value, long defaultValue) {
        String rawValue = AgenticCommerceWayangMaps.text(value);
        if (rawValue.isBlank()) {
            return new ParseReport(false, true, Math.max(0L, defaultValue), "", "");
        }
        OptionalLong parsed = parse(value);
        if (parsed.isPresent()) {
            return new ParseReport(true, true, parsed.getAsLong(), rawValue, "");
        }
        return new ParseReport(
                true,
                false,
                Math.max(0L, defaultValue),
                rawValue,
                ISSUE_INVALID_BYTE_SIZE);
    }

    static String format(long bytes) {
        long normalized = Math.max(0L, bytes);
        if (normalized == 0L) {
            return "0 B";
        }
        String[] units = {"B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB"};
        BigDecimal amount = BigDecimal.valueOf(normalized);
        int unitIndex = 0;
        while (amount.compareTo(BigDecimal.valueOf(1024L)) >= 0 && unitIndex < units.length - 1) {
            amount = amount.divide(BigDecimal.valueOf(1024L), 2, RoundingMode.HALF_UP);
            unitIndex++;
        }
        if (unitIndex == 0) {
            return normalized + " B";
        }
        return amount.setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
                + " "
                + units[unitIndex];
    }

    static String formatLimit(long bytes) {
        return bytes <= 0L ? "unlimited" : format(bytes);
    }

    private static int suffixIndex(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!Character.isDigit(character) && character != '.' && character != '-' && character != '+') {
                return index;
            }
        }
        return -1;
    }

    private static long multiplier(String unit) {
        return switch (unit) {
            case "", "b", "byte", "bytes" -> 1L;
            case "k", "kb" -> 1_000L;
            case "ki", "kib" -> 1_024L;
            case "m", "mb" -> 1_000_000L;
            case "mi", "mib" -> 1_048_576L;
            case "g", "gb" -> 1_000_000_000L;
            case "gi", "gib" -> 1_073_741_824L;
            case "t", "tb" -> 1_000_000_000_000L;
            case "ti", "tib" -> 1_099_511_627_776L;
            default -> throw new IllegalArgumentException("Unsupported byte-size unit: " + unit);
        };
    }

    record ParseReport(
            boolean configured,
            boolean valid,
            long bytes,
            String rawValue,
            String issue) {

        ParseReport {
            bytes = Math.max(0L, bytes);
            rawValue = AgenticCommerceWayangMaps.text(rawValue);
            issue = AgenticCommerceWayangMaps.text(issue);
        }

        boolean invalid() {
            return configured && !valid;
        }

        Map<String, Object> toMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("configured", configured);
            values.put("valid", valid);
            values.put("bytes", bytes);
            values.put("bytesDisplay", formatLimit(bytes));
            if (!rawValue.isBlank()) {
                values.put("rawValue", rawValue);
            }
            if (!issue.isBlank()) {
                values.put("issue", issue);
            }
            return Map.copyOf(values);
        }
    }
}

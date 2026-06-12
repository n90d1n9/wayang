package tech.kayys.wayang.a2ui.wayang.support;

/**
 * Small coercion helpers shared by A2UI decoders.
 */
public final class DecodeValues {

    public static String rawText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static String text(Object value) {
        return rawText(value).trim();
    }

    public static String text(Object value, String fallback) {
        String text = text(value);
        return text.isBlank() ? fallback == null ? "" : fallback : text;
    }

    public static boolean bool(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String text = text(value);
        return text.isBlank() ? fallback : Boolean.parseBoolean(text);
    }

    public static int nonNegativeInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        String text = text(value);
        if (text.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(0, Integer.parseInt(text));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static int clampedNonNegativeInt(Object value, int fallback) {
        long count = nonNegativeLong(value, fallback);
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.toIntExact(count);
    }

    public static long nonNegativeLong(Object value, long fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return Math.max(0L, number.longValue());
        }
        String text = text(value);
        if (text.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(0L, Long.parseLong(text));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static Integer integerOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = text(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private DecodeValues() {
    }
}

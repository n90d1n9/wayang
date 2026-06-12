package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;
import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import java.util.List;
import java.util.Objects;

/**
 * Strict scalar/list coercion for stored or remote A2UI HTTP report metrics.
 */
public final class HttpReportMetricDecoders {

    public static int intCount(Object value, String fieldName, String owner) {
        long count = count(value, fieldName, owner);
        try {
            return Math.toIntExact(count);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(owner + " count must fit int: " + fieldName, e);
        }
    }

    public static long count(Object value, String fieldName, String owner) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = DecodeValues.rawText(value).trim();
        if (text.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(owner + " count must be numeric: " + fieldName, e);
        }
    }

    public static List<Integer> integerList(Object value, String fieldName, String owner) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> integer(item, fieldName, owner))
                    .filter(Objects::nonNull)
                    .toList();
        }
        Integer integer = integer(value, fieldName, owner);
        return RecordCollections.singletonOrEmpty(integer);
    }

    private static Integer integer(Object value, String fieldName, String owner) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = DecodeValues.rawText(value).trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(owner + " integer list must be numeric: " + fieldName, e);
        }
    }

    private HttpReportMetricDecoders() {
    }
}

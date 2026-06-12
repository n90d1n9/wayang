package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;
import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared coercion for flexible framework HTTP header values.
 */
public final class HttpHeaderValues {

    private HttpHeaderValues() {
    }

    public static List<String> values(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Optional<?> optional) {
            return optional.map(HttpHeaderValues::values).orElse(List.of());
        }
        if (value instanceof Iterable<?> values) {
            return iterableValues(values);
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            return arrayValues(value);
        }
        String normalized = DecodeValues.text(value);
        return RecordCollections.singletonOrEmpty(normalized.isBlank() ? null : normalized);
    }

    public static String joined(Object value) {
        return String.join(", ", values(value));
    }

    private static List<String> iterableValues(Iterable<?> values) {
        List<String> normalized = new ArrayList<>();
        for (Object value : values) {
            normalized.addAll(values(value));
        }
        return List.copyOf(normalized);
    }

    private static List<String> arrayValues(Object values) {
        List<String> normalized = new ArrayList<>();
        int length = Array.getLength(values);
        for (int i = 0; i < length; i++) {
            normalized.addAll(values(Array.get(values, i)));
        }
        return List.copyOf(normalized);
    }
}

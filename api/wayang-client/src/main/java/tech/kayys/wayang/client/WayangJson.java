package tech.kayys.wayang.client;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

/**
 * Small dependency-free JSON writer for SDK-owned envelope maps.
 *
 * <p>The SDK uses ordered maps as its public wire shape so CLI, TUI, HTTP, and
 * future UI wrappers can render the same contracts without reimplementing JSON
 * escaping or drifting on field order.</p>
 */
public final class WayangJson {

    private WayangJson() {
    }

    /**
     * Renders an object-shaped map as compact JSON while preserving map iteration order.
     *
     * @param values object fields to render
     * @return compact JSON text
     */
    public static String object(Map<String, ?> values) {
        return value(values);
    }

    /**
     * Renders a supported Java value as compact JSON.
     *
     * <p>Supported structured values are {@link Map}, {@link Iterable}, and Java
     * arrays. Strings are escaped according to JSON rules. Non-finite floating
     * point values are rendered as strings so the result remains valid JSON.</p>
     *
     * @param value value to render
     * @return compact JSON text
     */
    public static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return quote(string);
        }
        if (value instanceof Double doubleValue) {
            return Double.isFinite(doubleValue) ? String.valueOf(doubleValue) : quote(String.valueOf(doubleValue));
        }
        if (value instanceof Float floatValue) {
            return Float.isFinite(floatValue) ? String.valueOf(floatValue) : quote(String.valueOf(floatValue));
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            return map(map);
        }
        if (value instanceof Iterable<?> iterable) {
            return iterable(iterable);
        }
        if (value.getClass().isArray()) {
            return array(value);
        }
        return quote(String.valueOf(value));
    }

    /**
     * Escapes a value as a JSON string literal.
     *
     * @param value raw string value, with null treated as an empty string
     * @return quoted JSON string literal
     */
    public static String quote(String value) {
        String normalized = value == null ? "" : value;
        StringBuilder output = new StringBuilder(normalized.length() + 2);
        output.append('"');
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            switch (ch) {
                case '\\' -> output.append("\\\\");
                case '"' -> output.append("\\\"");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> appendEscaped(output, ch);
            }
        }
        output.append('"');
        return output.toString();
    }

    private static void appendEscaped(StringBuilder output, char ch) {
        if (ch < 0x20) {
            output.append(String.format("\\u%04x", (int) ch));
        } else {
            output.append(ch);
        }
    }

    private static String map(Map<?, ?> map) {
        StringBuilder output = new StringBuilder("{");
        Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            output.append(quote(String.valueOf(entry.getKey())))
                    .append(':')
                    .append(value(entry.getValue()));
            if (iterator.hasNext()) {
                output.append(',');
            }
        }
        output.append('}');
        return output.toString();
    }

    private static String iterable(Iterable<?> iterable) {
        StringBuilder output = new StringBuilder("[");
        Iterator<?> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            output.append(value(iterator.next()));
            if (iterator.hasNext()) {
                output.append(',');
            }
        }
        output.append(']');
        return output.toString();
    }

    private static String array(Object array) {
        StringBuilder output = new StringBuilder("[");
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                output.append(',');
            }
            output.append(value(Array.get(array, i)));
        }
        output.append(']');
        return output.toString();
    }
}

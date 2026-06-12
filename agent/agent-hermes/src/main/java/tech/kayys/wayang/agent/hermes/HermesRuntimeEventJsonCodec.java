package tech.kayys.wayang.agent.hermes;

import java.lang.reflect.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small JSON encoder for runtime-event JSONL journals.
 */
final class HermesRuntimeEventJsonCodec {

    private HermesRuntimeEventJsonCodec() {
    }

    static String toJsonLine(HermesRuntimeEvent event) {
        return toJson(event == null ? Map.of() : event.toMetadata());
    }

    static String toJsonLine(Map<String, ?> values) {
        return toJson(values == null ? Map.of() : values);
    }

    static HermesRuntimeEvent fromJsonLine(String line) {
        return HermesRuntimeEvent.fromMetadata(objectFromJsonLine(line));
    }

    static Map<String, Object> objectFromJsonLine(String line) {
        Object value = new Parser(line).parse();
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Hermes JSON line must be a JSON object");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> values.put(String.valueOf(key), mapValue));
        return Collections.unmodifiableMap(values);
    }

    private static String toJson(Object value) {
        StringBuilder builder = new StringBuilder();
        appendJson(builder, value);
        return builder.toString();
    }

    private static void appendJson(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String text) {
            appendQuoted(builder, text);
        } else if (value instanceof Character character) {
            appendQuoted(builder, character.toString());
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Instant instant) {
            appendQuoted(builder, instant.toString());
        } else if (value instanceof Map<?, ?> map) {
            appendMap(builder, map);
        } else if (value instanceof Iterable<?> values) {
            appendIterable(builder, values);
        } else if (value.getClass().isArray()) {
            appendArray(builder, value);
        } else {
            appendQuoted(builder, String.valueOf(value));
        }
    }

    private static void appendMap(StringBuilder builder, Map<?, ?> map) {
        builder.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet().stream()
                .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                .toList()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            appendQuoted(builder, String.valueOf(entry.getKey()));
            builder.append(':');
            appendJson(builder, entry.getValue());
        }
        builder.append('}');
    }

    private static void appendIterable(StringBuilder builder, Iterable<?> values) {
        builder.append('[');
        boolean first = true;
        for (Object value : values) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            appendJson(builder, value);
        }
        builder.append(']');
    }

    private static void appendArray(StringBuilder builder, Object array) {
        builder.append('[');
        int length = Array.getLength(array);
        for (int index = 0; index < length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            appendJson(builder, Array.get(array, index));
        }
        builder.append(']');
    }

    private static void appendQuoted(StringBuilder builder, String text) {
        builder.append('"');
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            switch (value) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (value < 0x20) {
                        builder.append(String.format("\\u%04x", (int) value));
                    } else {
                        builder.append(value);
                    }
                }
            }
        }
        builder.append('"');
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input == null ? "" : input.trim();
        }

        private Object parse() {
            Object value = readValue();
            skipWhitespace();
            if (index != input.length()) {
                throw new IllegalArgumentException("Unexpected trailing JSON content");
            }
            return value;
        }

        private Object readValue() {
            skipWhitespace();
            if (index >= input.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON input");
            }
            char value = input.charAt(index);
            return switch (value) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readLiteral("true", Boolean.TRUE);
                case 'f' -> readLiteral("false", Boolean.FALSE);
                case 'n' -> readLiteral("null", null);
                default -> {
                    if (value == '-' || Character.isDigit(value)) {
                        yield readNumber();
                    }
                    throw new IllegalArgumentException("Unexpected JSON token: " + value);
                }
            };
        }

        private Map<String, Object> readObject() {
            expect('{');
            Map<String, Object> values = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return values;
            }
            while (true) {
                String key = readString();
                skipWhitespace();
                expect(':');
                values.put(key, readValue());
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return values;
                }
                expect(',');
            }
        }

        private List<Object> readArray() {
            expect('[');
            List<Object> values = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return values;
            }
            while (true) {
                values.add(readValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return values;
                }
                expect(',');
            }
        }

        private String readString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < input.length()) {
                char value = input.charAt(index++);
                if (value == '"') {
                    return builder.toString();
                }
                if (value != '\\') {
                    builder.append(value);
                    continue;
                }
                if (index >= input.length()) {
                    throw new IllegalArgumentException("Unterminated JSON escape sequence");
                }
                char escaped = input.charAt(index++);
                switch (escaped) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> builder.append(readUnicode());
                    default -> throw new IllegalArgumentException("Unsupported JSON escape: " + escaped);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private char readUnicode() {
            if (index + 4 > input.length()) {
                throw new IllegalArgumentException("Incomplete JSON unicode escape");
            }
            String hex = input.substring(index, index + 4);
            index += 4;
            return (char) Integer.parseInt(hex, 16);
        }

        private Object readNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            readDigits();
            boolean decimal = false;
            if (peek('.')) {
                decimal = true;
                index++;
                readDigits();
            }
            if (peek('e') || peek('E')) {
                decimal = true;
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                readDigits();
            }
            String number = input.substring(start, index);
            return decimal ? Double.parseDouble(number) : Long.parseLong(number);
        }

        private void readDigits() {
            int start = index;
            while (index < input.length() && Character.isDigit(input.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw new IllegalArgumentException("Expected JSON number digits");
            }
        }

        private Object readLiteral(String literal, Object value) {
            if (!input.startsWith(literal, index)) {
                throw new IllegalArgumentException("Expected JSON literal " + literal);
            }
            index += literal.length();
            return value;
        }

        private boolean peek(char value) {
            return index < input.length() && input.charAt(index) == value;
        }

        private void expect(char value) {
            skipWhitespace();
            if (!peek(value)) {
                throw new IllegalArgumentException("Expected JSON token: " + value);
            }
            index++;
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }
    }
}

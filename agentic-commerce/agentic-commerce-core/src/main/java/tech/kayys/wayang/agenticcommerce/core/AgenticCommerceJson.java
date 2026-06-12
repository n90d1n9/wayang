package tech.kayys.wayang.agenticcommerce.core;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny JSON writer for dependency-free Agentic Commerce protocol payloads.
 */
public final class AgenticCommerceJson {

    private AgenticCommerceJson() {
    }

    public static String write(Object value) {
        StringBuilder json = new StringBuilder();
        append(json, value);
        return json.toString();
    }

    public static Object read(String json) {
        return new Parser(json).parse();
    }

    public static Map<String, Object> readObject(String json) {
        Object value = read(json);
        if (value instanceof Map<?, ?> map) {
            return AgenticCommerceMaps.copy(map);
        }
        throw new IllegalArgumentException("Agentic Commerce JSON payload must be an object");
    }

    private static void append(StringBuilder json, Object value) {
        Object copied = AgenticCommerceMaps.copyValue(value);
        if (copied == null) {
            json.append("null");
        } else if (copied instanceof String string) {
            appendString(json, string);
        } else if (copied instanceof Number number) {
            appendNumber(json, number);
        } else if (copied instanceof Boolean bool) {
            json.append(bool);
        } else if (copied instanceof Map<?, ?> map) {
            appendMap(json, map);
        } else if (copied instanceof List<?> list) {
            appendList(json, list);
        } else {
            appendString(json, String.valueOf(copied));
        }
    }

    private static void appendMap(StringBuilder json, Map<?, ?> map) {
        json.append('{');
        Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            appendString(json, String.valueOf(entry.getKey()));
            json.append(':');
            append(json, entry.getValue());
            if (iterator.hasNext()) {
                json.append(',');
            }
        }
        json.append('}');
    }

    private static void appendList(StringBuilder json, List<?> list) {
        json.append('[');
        Iterator<?> iterator = list.iterator();
        while (iterator.hasNext()) {
            append(json, iterator.next());
            if (iterator.hasNext()) {
                json.append(',');
            }
        }
        json.append(']');
    }

    private static void appendNumber(StringBuilder json, Number number) {
        double numeric = number.doubleValue();
        if (Double.isFinite(numeric)) {
            json.append(number);
        } else {
            json.append('0');
        }
    }

    private static void appendString(StringBuilder json, String value) {
        json.append('"');
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (current < 0x20) {
                        json.append(String.format("\\u%04x", (int) current));
                    } else {
                        json.append(current);
                    }
                }
            }
        }
        json.append('"');
    }

    private static final class Parser {

        private final String json;
        private int index;

        private Parser(String json) {
            this.json = json == null ? "" : json.trim();
        }

        private Object parse() {
            if (json.isBlank()) {
                throw error("JSON payload is blank");
            }
            Object value = value();
            whitespace();
            if (!end()) {
                throw error("Unexpected trailing JSON content");
            }
            return value;
        }

        private Object value() {
            whitespace();
            if (end()) {
                throw error("Unexpected end of JSON payload");
            }
            char current = peek();
            return switch (current) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            expect('{');
            Map<String, Object> values = new LinkedHashMap<>();
            whitespace();
            if (consume('}')) {
                return AgenticCommerceMaps.copy(values);
            }
            while (true) {
                whitespace();
                String key = string();
                whitespace();
                expect(':');
                Object value = value();
                if (value != null) {
                    values.put(key, value);
                }
                whitespace();
                if (consume('}')) {
                    return AgenticCommerceMaps.copy(values);
                }
                expect(',');
            }
        }

        private List<Object> array() {
            expect('[');
            java.util.ArrayList<Object> values = new java.util.ArrayList<>();
            whitespace();
            if (consume(']')) {
                return List.of();
            }
            while (true) {
                Object value = value();
                if (value != null) {
                    values.add(value);
                }
                whitespace();
                if (consume(']')) {
                    return List.copyOf(values);
                }
                expect(',');
            }
        }

        private String string() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (!end()) {
                char current = next();
                if (current == '"') {
                    return value.toString();
                }
                if (current == '\\') {
                    value.append(escape());
                } else {
                    value.append(current);
                }
            }
            throw error("Unterminated JSON string");
        }

        private char escape() {
            if (end()) {
                throw error("Unterminated JSON escape");
            }
            char current = next();
            return switch (current) {
                case '"' -> '"';
                case '\\' -> '\\';
                case '/' -> '/';
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> unicode();
                default -> throw error("Unsupported JSON escape");
            };
        }

        private char unicode() {
            if (index + 4 > json.length()) {
                throw error("Incomplete JSON unicode escape");
            }
            String digits = json.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(digits, 16);
            } catch (NumberFormatException exception) {
                throw error("Invalid JSON unicode escape");
            }
        }

        private Object number() {
            int start = index;
            consume('-');
            digits();
            boolean decimal = false;
            if (consume('.')) {
                decimal = true;
                digits();
            }
            if (!end() && (peek() == 'e' || peek() == 'E')) {
                decimal = true;
                next();
                if (!end() && (peek() == '+' || peek() == '-')) {
                    next();
                }
                digits();
            }
            String token = json.substring(start, index);
            try {
                if (decimal) {
                    return Double.parseDouble(token);
                }
                return Long.parseLong(token);
            } catch (NumberFormatException exception) {
                throw error("Invalid JSON number");
            }
        }

        private void digits() {
            int start = index;
            while (!end() && Character.isDigit(peek())) {
                next();
            }
            if (start == index) {
                throw error("Expected JSON digit");
            }
        }

        private Object literal(String literal, Object value) {
            if (!json.startsWith(literal, index)) {
                throw error("Invalid JSON literal");
            }
            index += literal.length();
            return value;
        }

        private void whitespace() {
            while (!end() && Character.isWhitespace(peek())) {
                index++;
            }
        }

        private void expect(char expected) {
            if (end() || next() != expected) {
                throw error("Expected '" + expected + "'");
            }
        }

        private boolean consume(char expected) {
            if (!end() && peek() == expected) {
                index++;
                return true;
            }
            return false;
        }

        private char peek() {
            return json.charAt(index);
        }

        private char next() {
            return json.charAt(index++);
        }

        private boolean end() {
            return index >= json.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + index);
        }
    }
}

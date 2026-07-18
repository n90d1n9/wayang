package tech.kayys.wayang.gollek.cli;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TestJson {

    private TestJson() {
    }

    static Object parse(String json) {
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.complete()) {
            throw parser.error("Unexpected trailing content");
        }
        return value;
    }

    private static final class Parser {

        private final String json;
        private int index;

        Parser(String json) {
            this.json = json == null ? "" : json;
        }

        Object parseValue() {
            skipWhitespace();
            if (complete()) {
                throw error("Expected JSON value");
            }
            char ch = current();
            return switch (ch) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (ch == '-' || Character.isDigit(ch)) {
                        yield parseNumber();
                    }
                    throw error("Unexpected JSON token '" + ch + "'");
                }
            };
        }

        void skipWhitespace() {
            while (!complete() && Character.isWhitespace(current())) {
                index++;
            }
        }

        boolean complete() {
            return index >= json.length();
        }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at offset " + index);
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> values = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return values;
            }
            while (true) {
                skipWhitespace();
                if (complete() || current() != '"') {
                    throw error("Expected object key");
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                values.put(key, parseValue());
                skipWhitespace();
                if (consume('}')) {
                    return values;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> values = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return values;
            }
            while (true) {
                values.add(parseValue());
                skipWhitespace();
                if (consume(']')) {
                    return values;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (!complete()) {
                char ch = current();
                index++;
                if (ch == '"') {
                    return value.toString();
                }
                if (ch != '\\') {
                    value.append(ch);
                    continue;
                }
                if (complete()) {
                    throw error("Unterminated escape sequence");
                }
                char escaped = current();
                index++;
                switch (escaped) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case '/' -> value.append('/');
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(parseUnicodeEscape());
                    default -> throw error("Unsupported escape sequence '\\" + escaped + "'");
                }
            }
            throw error("Unterminated string");
        }

        private char parseUnicodeEscape() {
            if (index + 4 > json.length()) {
                throw error("Incomplete unicode escape");
            }
            String hex = json.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException ex) {
                throw error("Invalid unicode escape");
            }
        }

        private BigDecimal parseNumber() {
            int start = index;
            consume('-');
            consumeDigits();
            if (consume('.')) {
                consumeDigits();
            }
            if (!complete() && (current() == 'e' || current() == 'E')) {
                index++;
                if (!complete() && (current() == '+' || current() == '-')) {
                    index++;
                }
                consumeDigits();
            }
            return new BigDecimal(json.substring(start, index));
        }

        private void consumeDigits() {
            int start = index;
            while (!complete() && Character.isDigit(current())) {
                index++;
            }
            if (start == index) {
                throw error("Expected digit");
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!json.startsWith(literal, index)) {
                throw error("Expected '" + literal + "'");
            }
            index += literal.length();
            return value;
        }

        private char current() {
            return json.charAt(index);
        }

        private boolean consume(char expected) {
            if (!complete() && current() == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                throw error("Expected '" + expected + "'");
            }
        }
    }
}

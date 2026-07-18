package tech.kayys.wayang.json;

import java.util.*;

/**
 * A minimal, dependency-free JSON value + parser + writer.
 *
 * Why hand-rolled: this project intentionally has zero external
 * dependencies (no Jackson/Gson) so it compiles with nothing but a
 * stock JDK. The subset of JSON needed to talk to LLM APIs and to
 * read/write config files is small, so a ~300-line implementation
 * is plenty and keeps the whole project a single `javac` away from
 * running on any machine.
 */
public final class Json {

    private Json() {}

    public static JsonValue parse(String text) {
        Parser p = new Parser(text);
        JsonValue v = p.parseValue();
        p.skipWhitespace();
        if (!p.atEnd()) {
            throw new JsonException("Trailing content after JSON value at position " + p.pos);
        }
        return v;
    }

    public static String write(JsonValue value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb);
        return sb.toString();
    }

    public static String writePretty(JsonValue value) {
        StringBuilder sb = new StringBuilder();
        writeValuePretty(value, sb, 0);
        return sb.toString();
    }

    // ---------- writer ----------

    private static void writeValue(JsonValue v, StringBuilder sb) {
        switch (v.type()) {
            case NULL -> sb.append("null");
            case BOOL -> sb.append(v.asBoolean() ? "true" : "false");
            case NUMBER -> writeNumber(v.asDouble(), sb);
            case STRING -> writeString(v.asString(), sb);
            case ARRAY -> {
                sb.append('[');
                List<JsonValue> items = v.asArray();
                for (int i = 0; i < items.size(); i++) {
                    if (i > 0) sb.append(',');
                    writeValue(items.get(i), sb);
                }
                sb.append(']');
            }
            case OBJECT -> {
                sb.append('{');
                boolean first = true;
                for (Map.Entry<String, JsonValue> e : v.asObject().entrySet()) {
                    if (!first) sb.append(',');
                    first = false;
                    writeString(e.getKey(), sb);
                    sb.append(':');
                    writeValue(e.getValue(), sb);
                }
                sb.append('}');
            }
        }
    }

    private static void writeValuePretty(JsonValue v, StringBuilder sb, int indent) {
        switch (v.type()) {
            case ARRAY -> {
                List<JsonValue> items = v.asArray();
                if (items.isEmpty()) { sb.append("[]"); return; }
                sb.append("[\n");
                for (int i = 0; i < items.size(); i++) {
                    pad(sb, indent + 2);
                    writeValuePretty(items.get(i), sb, indent + 2);
                    if (i < items.size() - 1) sb.append(',');
                    sb.append('\n');
                }
                pad(sb, indent);
                sb.append(']');
            }
            case OBJECT -> {
                Map<String, JsonValue> obj = v.asObject();
                if (obj.isEmpty()) { sb.append("{}"); return; }
                sb.append("{\n");
                int i = 0, n = obj.size();
                for (Map.Entry<String, JsonValue> e : obj.entrySet()) {
                    pad(sb, indent + 2);
                    writeString(e.getKey(), sb);
                    sb.append(": ");
                    writeValuePretty(e.getValue(), sb, indent + 2);
                    if (++i < n) sb.append(',');
                    sb.append('\n');
                }
                pad(sb, indent);
                sb.append('}');
            }
            default -> writeValue(v, sb);
        }
    }

    private static void pad(StringBuilder sb, int n) {
        sb.append(" ".repeat(Math.max(0, n)));
    }

    private static void writeNumber(double d, StringBuilder sb) {
        if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
            sb.append((long) d);
        } else {
            sb.append(d);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ---------- parser ----------

    public static final class JsonException extends RuntimeException {
        public JsonException(String msg) { super(msg); }
    }

    private static final class Parser {
        final String s;
        int pos = 0;

        Parser(String s) { this.s = s; }

        boolean atEnd() { return pos >= s.length(); }

        char peek() {
            if (atEnd()) throw new JsonException("Unexpected end of input");
            return s.charAt(pos);
        }

        char next() {
            char c = peek();
            pos++;
            return c;
        }

        void skipWhitespace() {
            while (!atEnd()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
                else break;
            }
        }

        void expect(char c) {
            skipWhitespace();
            if (atEnd() || s.charAt(pos) != c) {
                throw new JsonException("Expected '" + c + "' at position " + pos +
                        " but found " + (atEnd() ? "<eof>" : s.charAt(pos)));
            }
            pos++;
        }

        JsonValue parseValue() {
            skipWhitespace();
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> JsonValue.of(parseString());
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        JsonValue parseObject() {
            expect('{');
            LinkedHashMap<String, JsonValue> map = new LinkedHashMap<>();
            skipWhitespace();
            if (!atEnd() && peek() == '}') { pos++; return JsonValue.of(map); }
            while (true) {
                skipWhitespace();
                String key = parseString();
                expect(':');
                JsonValue val = parseValue();
                map.put(key, val);
                skipWhitespace();
                char c = next();
                if (c == '}') break;
                if (c != ',') throw new JsonException("Expected ',' or '}' at position " + (pos - 1));
            }
            return JsonValue.of(map);
        }

        JsonValue parseArray() {
            expect('[');
            List<JsonValue> list = new ArrayList<>();
            skipWhitespace();
            if (!atEnd() && peek() == ']') { pos++; return JsonValue.of(list); }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                char c = next();
                if (c == ']') break;
                if (c != ',') throw new JsonException("Expected ',' or ']' at position " + (pos - 1));
            }
            return JsonValue.of(list);
        }

        String parseString() {
            skipWhitespace();
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') break;
                if (c == '\\') {
                    char esc = next();
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            String hex = s.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                        }
                        default -> throw new JsonException("Invalid escape \\" + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        JsonValue parseBoolean() {
            if (s.startsWith("true", pos)) { pos += 4; return JsonValue.of(true); }
            if (s.startsWith("false", pos)) { pos += 5; return JsonValue.of(false); }
            throw new JsonException("Invalid literal at position " + pos);
        }

        JsonValue parseNull() {
            if (s.startsWith("null", pos)) { pos += 4; return JsonValue.NULL; }
            throw new JsonException("Invalid literal at position " + pos);
        }

        JsonValue parseNumber() {
            int start = pos;
            if (!atEnd() && peek() == '-') pos++;
            while (!atEnd() && Character.isDigit(peek())) pos++;
            if (!atEnd() && peek() == '.') {
                pos++;
                while (!atEnd() && Character.isDigit(peek())) pos++;
            }
            if (!atEnd() && (peek() == 'e' || peek() == 'E')) {
                pos++;
                if (!atEnd() && (peek() == '+' || peek() == '-')) pos++;
                while (!atEnd() && Character.isDigit(peek())) pos++;
            }
            if (pos == start) throw new JsonException("Invalid number at position " + pos);
            return JsonValue.of(Double.parseDouble(s.substring(start, pos)));
        }
    }
}

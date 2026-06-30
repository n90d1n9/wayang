package tech.kayys.wayang.sdk.json;

import java.util.*;

/**
 * A JSON value: object, array, string, number, boolean, or null.
 * Designed for ergonomic chaining, e.g.:
 *   json.get("content").get(0).get("text").asString()
 */
public final class JsonValue {

    public enum Type { OBJECT, ARRAY, STRING, NUMBER, BOOL, NULL }

    public static final JsonValue NULL = new JsonValue(Type.NULL, null);

    private final Type type;
    private final Object value;

    private JsonValue(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    // ---------- factories ----------

    public static JsonValue of(String s) { return s == null ? NULL : new JsonValue(Type.STRING, s); }
    public static JsonValue of(double d) { return new JsonValue(Type.NUMBER, d); }
    public static JsonValue of(int i) { return new JsonValue(Type.NUMBER, (double) i); }
    public static JsonValue of(long l) { return new JsonValue(Type.NUMBER, (double) l); }
    public static JsonValue of(boolean b) { return new JsonValue(Type.BOOL, b); }
    public static JsonValue of(Map<String, JsonValue> m) { return new JsonValue(Type.OBJECT, m); }
    public static JsonValue of(List<JsonValue> l) { return new JsonValue(Type.ARRAY, l); }

    public static JsonValue object() { return new JsonValue(Type.OBJECT, new LinkedHashMap<String, JsonValue>()); }
    public static JsonValue array() { return new JsonValue(Type.ARRAY, new ArrayList<JsonValue>()); }

    // ---------- type info ----------

    public Type type() { return type; }
    public boolean isNull() { return type == Type.NULL; }
    public boolean isObject() { return type == Type.OBJECT; }
    public boolean isArray() { return type == Type.ARRAY; }
    public boolean isString() { return type == Type.STRING; }

    // ---------- accessors ----------

    @SuppressWarnings("unchecked")
    public Map<String, JsonValue> asObject() {
        if (type != Type.OBJECT) throw new IllegalStateException("Not an object: " + type);
        return (Map<String, JsonValue>) value;
    }

    @SuppressWarnings("unchecked")
    public List<JsonValue> asArray() {
        if (type != Type.ARRAY) throw new IllegalStateException("Not an array: " + type);
        return (List<JsonValue>) value;
    }

    public String asString() {
        if (type == Type.NULL) return null;
        if (type != Type.STRING) throw new IllegalStateException("Not a string: " + type);
        return (String) value;
    }

    public String asString(String fallback) {
        return type == Type.STRING ? (String) value : fallback;
    }

    public double asDouble() {
        if (type != Type.NUMBER) throw new IllegalStateException("Not a number: " + type);
        return (Double) value;
    }

    public int asInt() { return (int) asDouble(); }

    public boolean asBoolean() {
        if (type != Type.BOOL) throw new IllegalStateException("Not a boolean: " + type);
        return (Boolean) value;
    }

    public boolean asBoolean(boolean fallback) {
        return type == Type.BOOL ? (Boolean) value : fallback;
    }

    /** Object field lookup; returns JsonValue.NULL if missing or not an object. */
    public JsonValue get(String key) {
        if (type != Type.OBJECT) return NULL;
        JsonValue v = asObject().get(key);
        return v == null ? NULL : v;
    }

    /** Array index lookup; returns JsonValue.NULL if out of range or not an array. */
    public JsonValue get(int index) {
        if (type != Type.ARRAY) return NULL;
        List<JsonValue> arr = asArray();
        return (index >= 0 && index < arr.size()) ? arr.get(index) : NULL;
    }

    public boolean has(String key) {
        return type == Type.OBJECT && asObject().containsKey(key);
    }

    /**
     * Converts an OBJECT JsonValue to a {@code Map<String, Object>} suitable for
     * passing to {@code tech.kayys.wayang.tools.spi.Tool#execute(Map, ToolContext)}.
     * Each value is recursively converted: OBJECT→Map, ARRAY→List, STRING→String,
     * NUMBER→Double, BOOL→Boolean, NULL→null.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> asStringObjectMap() {
        if (type != Type.OBJECT) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonValue> e : asObject().entrySet()) {
            result.put(e.getKey(), toJavaValue(e.getValue()));
        }
        return result;
    }

    private static Object toJavaValue(JsonValue v) {
        return switch (v.type) {
            case OBJECT -> v.asStringObjectMap();
            case ARRAY  -> v.asArray().stream().map(JsonValue::toJavaValue).toList();
            case STRING -> v.value;
            case NUMBER -> v.value;  // Double
            case BOOL   -> v.value;  // Boolean
            case NULL   -> null;
        };
    }

    public int size() {
        if (type == Type.ARRAY) return asArray().size();
        if (type == Type.OBJECT) return asObject().size();
        return 0;
    }

    // ---------- conversion from plain Java types ----------

    /**
     * Recursively converts a plain Java object (Map, List, String, Number, Boolean, null)
     * to a JsonValue. Useful for bridging tool inputSchema maps into request bodies.
     */
    @SuppressWarnings("unchecked")
    public static JsonValue fromJavaValue(Object o) {
        if (o == null) return NULL;
        if (o instanceof JsonValue jv) return jv;
        if (o instanceof Map<?,?> m) {
            JsonValue obj = object();
            for (Map.Entry<?,?> e : m.entrySet()) {
                obj.put(String.valueOf(e.getKey()), fromJavaValue(e.getValue()));
            }
            return obj;
        }
        if (o instanceof List<?> l) {
            JsonValue arr = array();
            for (Object item : l) arr.add(fromJavaValue(item));
            return arr;
        }
        if (o instanceof String s) return of(s);
        if (o instanceof Boolean b) return of(b);
        if (o instanceof Integer i) return of(i);
        if (o instanceof Long l) return of((double) l);
        if (o instanceof Number n) return of(n.doubleValue());
        return of(o.toString());
    }

    // ---------- mutation helpers (for building request bodies) ----------

    public JsonValue put(String key, JsonValue v) {
        asObject().put(key, v);
        return this;
    }

    public JsonValue put(String key, String v)  { return put(key, of(v)); }
    public JsonValue put(String key, double v)  { return put(key, of(v)); }
    public JsonValue put(String key, int v)     { return put(key, of(v)); }
    public JsonValue put(String key, boolean v) { return put(key, of(v)); }

    /** Convenience: put a plain Java Map (e.g. a tool's inputSchema) as a nested JSON object. */
    public JsonValue put(String key, Map<?,?> v) { return put(key, fromJavaValue(v)); }

    public JsonValue add(JsonValue v) {
        asArray().add(v);
        return this;
    }

    @Override
    public String toString() { return Json.write(this); }
}

package tech.kayys.wayang.tool.os;

import java.util.*;

/** Tiny builder for the JSON Schema subset needed to describe tool inputs to an LLM. */
public final class Schema {

    private Schema() {}

    public static Map<String, Object> object(Map<String, Object> properties, String... required) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "object");
        s.put("properties", properties);
        if (required.length > 0) {
            s.put("required", Arrays.asList(required));
        }
        return s;
    }

    public static Map<String, Object> string(String description) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "string");
        s.put("description", description);
        return s;
    }

    public static Map<String, Object> integer(String description) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "integer");
        s.put("description", description);
        return s;
    }

    public static Map<String, Object> bool(String description) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", "boolean");
        s.put("description", description);
        return s;
    }

    public static Map<String, Object> props(Object... kv) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}

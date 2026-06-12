package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON-RPC 2.0 request envelope for A2A methods.
 */
public record WayangA2aJsonRpcRequest(Object id, String method, Map<String, Object> params) {

    public static final String VERSION = "2.0";

    public WayangA2aJsonRpcRequest {
        method = WayangA2aMaps.required(method, "method");
        params = WayangA2aMaps.copyMap(params);
    }

    public static WayangA2aJsonRpcRequest of(Object id, String method, Map<String, Object> params) {
        return new WayangA2aJsonRpcRequest(id, method, params);
    }

    public static WayangA2aJsonRpcRequest fromMap(Map<?, ?> payload) {
        Map<String, Object> values = WayangA2aMaps.copyMap(payload);
        String version = WayangA2aMaps.optional(values.get("jsonrpc"));
        if (!VERSION.equals(version)) {
            throw new IllegalArgumentException("JSON-RPC request must declare jsonrpc 2.0");
        }
        return new WayangA2aJsonRpcRequest(
                values.get("id"),
                WayangA2aMaps.required(WayangA2aMaps.optional(values.get("method")), "method"),
                params(values.get("params")));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("jsonrpc", VERSION);
        if (id != null) {
            values.put("id", id);
        }
        values.put("method", method);
        if (!params.isEmpty()) {
            values.put("params", params);
        }
        return WayangA2aMaps.copyMap(values);
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }

    private static Map<String, Object> params(Object raw) {
        if (raw == null) {
            return Map.of();
        }
        if (raw instanceof Map<?, ?> map) {
            return WayangA2aMaps.copyMap(map);
        }
        throw new IllegalArgumentException("JSON-RPC params must be an object");
    }
}

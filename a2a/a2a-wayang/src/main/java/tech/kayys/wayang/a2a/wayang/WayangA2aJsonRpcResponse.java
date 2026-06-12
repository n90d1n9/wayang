package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON-RPC 2.0 response envelope for A2A methods.
 */
public record WayangA2aJsonRpcResponse(Object id, Map<String, Object> result, WayangA2aJsonRpcError error) {

    public WayangA2aJsonRpcResponse {
        result = WayangA2aMaps.copyMap(result);
        int present = (result.isEmpty() ? 0 : 1) + (error == null ? 0 : 1);
        if (present != 1) {
            throw new IllegalArgumentException("JSON-RPC response requires exactly one result or error");
        }
    }

    public static WayangA2aJsonRpcResponse result(Object id, Map<String, Object> result) {
        return new WayangA2aJsonRpcResponse(id, result, null);
    }

    public static WayangA2aJsonRpcResponse empty(Object id) {
        return result(id, Map.of("empty", true));
    }

    public static WayangA2aJsonRpcResponse error(Object id, WayangA2aJsonRpcError error) {
        return new WayangA2aJsonRpcResponse(id, Map.of(), error);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("jsonrpc", WayangA2aJsonRpcRequest.VERSION);
        if (id != null) {
            values.put("id", id);
        }
        if (error == null) {
            values.put("result", result);
        } else {
            values.put("error", error.toMap());
        }
        return WayangA2aMaps.copyMap(values);
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }

    public String toEvent() {
        return "data: " + toJson() + "\n\n";
    }
}

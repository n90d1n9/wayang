package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Records when a later JSON-RPC handler group replaces an earlier method handler.
 */
record WayangA2aJsonRpcMethodHandlerOverride(
        String method,
        String originalGroup,
        String replacementGroup) {

    WayangA2aJsonRpcMethodHandlerOverride {
        method = WayangA2aMaps.required(method, "method");
        originalGroup = WayangA2aMaps.required(originalGroup, "originalGroup");
        replacementGroup = WayangA2aMaps.required(replacementGroup, "replacementGroup");
    }

    Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("method", method);
        values.put("originalGroup", originalGroup);
        values.put("replacementGroup", replacementGroup);
        return WayangA2aMaps.copyMap(values);
    }
}

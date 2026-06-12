package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parser and ordered projection helpers for A2A JSON-RPC method dispatch group coverage.
 */
final class WayangA2aJsonRpcMethodDispatchGroupCoverageProjection {

    private WayangA2aJsonRpcMethodDispatchGroupCoverageProjection() {
    }

    static WayangA2aJsonRpcMethodDispatchGroupCoverage fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        return new WayangA2aJsonRpcMethodDispatchGroupCoverage(
                WayangA2aMaps.optional(copy.get("group")),
                WayangA2aMaps.stringList(copy.get("registeredMethods")),
                WayangA2aMaps.stringList(copy.get("dispatchMethods")),
                WayangA2aMaps.stringList(copy.get("missingDispatchMethods")),
                WayangA2aMaps.stringList(copy.get("orphanDispatchMethods")));
    }

    static Map<String, Object> group(WayangA2aJsonRpcMethodDispatchGroupCoverage group) {
        WayangA2aJsonRpcMethodDispatchGroupCoverage resolved = Objects.requireNonNull(group, "group");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("group", resolved.group());
        values.put("complete", resolved.complete());
        values.put("registeredMethodCount", resolved.registeredMethodCount());
        values.put("dispatchMethodCount", resolved.dispatchMethodCount());
        values.put("registeredMethods", resolved.registeredMethods());
        values.put("dispatchMethods", resolved.dispatchMethods());
        values.put("missingDispatchMethods", resolved.missingDispatchMethods());
        values.put("orphanDispatchMethods", resolved.orphanDispatchMethods());
        return WayangA2aMaps.copyMap(values);
    }
}

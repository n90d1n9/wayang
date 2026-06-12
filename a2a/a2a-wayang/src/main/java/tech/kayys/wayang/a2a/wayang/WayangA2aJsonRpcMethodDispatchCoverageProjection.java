package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parser and ordered projection helpers for A2A JSON-RPC method dispatch coverage.
 */
final class WayangA2aJsonRpcMethodDispatchCoverageProjection {

    private WayangA2aJsonRpcMethodDispatchCoverageProjection() {
    }

    static WayangA2aJsonRpcMethodDispatchCoverage fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        List<String> registeredMethods = WayangA2aMaps.stringList(copy.get("registeredMethods"));
        List<String> dispatchMethods = WayangA2aMaps.stringList(copy.get("dispatchMethods"));
        boolean reported = reported(copy);
        List<WayangA2aJsonRpcMethodDispatchGroupCoverage> methodGroups = methodGroups(copy.get("methodGroups"));
        if (methodGroups.isEmpty() && reported) {
            methodGroups = WayangA2aJsonRpcMethodDispatchCoverage.from(
                    registeredMethods,
                    dispatchMethods).methodGroups();
        }
        return new WayangA2aJsonRpcMethodDispatchCoverage(
                reported,
                registeredMethods,
                dispatchMethods,
                WayangA2aMaps.stringList(copy.get("missingDispatchMethods")),
                WayangA2aMaps.stringList(copy.get("orphanDispatchMethods")),
                methodGroups);
    }

    static Map<String, Object> coverage(WayangA2aJsonRpcMethodDispatchCoverage coverage) {
        WayangA2aJsonRpcMethodDispatchCoverage resolved = Objects.requireNonNull(coverage, "coverage");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("complete", resolved.complete());
        values.put("registeredMethodCount", resolved.registeredMethodCount());
        values.put("dispatchMethodCount", resolved.dispatchMethodCount());
        values.put("registeredMethods", resolved.registeredMethods());
        values.put("dispatchMethods", resolved.dispatchMethods());
        values.put("missingDispatchMethods", resolved.missingDispatchMethods());
        values.put("orphanDispatchMethods", resolved.orphanDispatchMethods());
        values.put("methodGroups", methodGroups(resolved));
        return WayangA2aMaps.copyMap(values);
    }

    static List<Map<String, Object>> methodGroups(WayangA2aJsonRpcMethodDispatchCoverage coverage) {
        WayangA2aJsonRpcMethodDispatchCoverage resolved = Objects.requireNonNull(coverage, "coverage");
        return resolved.methodGroups().stream()
                .map(WayangA2aJsonRpcMethodDispatchGroupCoverage::toMap)
                .toList();
    }

    private static boolean reported(Map<String, Object> values) {
        return values.containsKey("registeredMethods")
                || values.containsKey("dispatchMethods")
                || values.containsKey("missingDispatchMethods")
                || values.containsKey("orphanDispatchMethods")
                || values.containsKey("methodGroups");
    }

    private static List<WayangA2aJsonRpcMethodDispatchGroupCoverage> methodGroups(Object value) {
        return WayangA2aMaps.objectList(value).stream()
                .map(WayangA2aJsonRpcMethodDispatchGroupCoverage::fromMap)
                .toList();
    }
}

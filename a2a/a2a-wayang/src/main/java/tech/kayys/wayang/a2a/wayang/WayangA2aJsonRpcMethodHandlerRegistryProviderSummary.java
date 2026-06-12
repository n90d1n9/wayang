package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provider-level summary derived from JSON-RPC method handler registry groups.
 */
record WayangA2aJsonRpcMethodHandlerRegistryProviderSummary(
        List<String> providerIds,
        List<String> moduleIds,
        List<String> capabilityTags) {

    WayangA2aJsonRpcMethodHandlerRegistryProviderSummary {
        providerIds = WayangA2aMaps.stringList(providerIds);
        moduleIds = WayangA2aMaps.stringList(moduleIds);
        capabilityTags = WayangA2aMaps.stringList(capabilityTags);
    }

    static WayangA2aJsonRpcMethodHandlerRegistryProviderSummary from(
            List<Map<String, Object>> groups,
            List<String> providerIds,
            List<String> moduleIds,
            List<String> capabilityTags) {
        List<Map<String, Object>> resolvedGroups = groups == null ? List.of() : groups;
        return new WayangA2aJsonRpcMethodHandlerRegistryProviderSummary(
                providerIds == null ? providerIds(resolvedGroups) : providerIds,
                moduleIds == null ? moduleIds(resolvedGroups) : moduleIds,
                capabilityTags == null ? capabilityTags(resolvedGroups) : capabilityTags);
    }

    static WayangA2aJsonRpcMethodHandlerRegistryProviderSummary fromMap(
            Map<String, Object> values,
            List<Map<String, Object>> groups) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        return from(
                groups,
                stringList(copy, "providerIds"),
                stringList(copy, "moduleIds"),
                stringList(copy, "capabilityTags"));
    }

    int providerCount() {
        return providerIds.size();
    }

    Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("providerCount", providerCount());
        values.put("providerIds", providerIds);
        values.put("moduleIds", moduleIds);
        values.put("capabilityTags", capabilityTags);
        return WayangA2aMaps.copyMap(values);
    }

    static boolean reported(Map<String, Object> values) {
        return values.containsKey("providerIds")
                || values.containsKey("moduleIds")
                || values.containsKey("capabilityTags")
                || values.containsKey("providerCount");
    }

    private static List<String> providerIds(List<Map<String, Object>> groups) {
        return groups.stream()
                .map(WayangA2aJsonRpcMethodHandlerRegistryProviderSummary::contribution)
                .map(contribution -> WayangA2aMaps.optional(contribution.get("providerId")))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static List<String> moduleIds(List<Map<String, Object>> groups) {
        return groups.stream()
                .map(WayangA2aJsonRpcMethodHandlerRegistryProviderSummary::contribution)
                .map(contribution -> WayangA2aMaps.optional(contribution.get("moduleId")))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static List<String> capabilityTags(List<Map<String, Object>> groups) {
        return groups.stream()
                .map(WayangA2aJsonRpcMethodHandlerRegistryProviderSummary::contribution)
                .flatMap(contribution -> WayangA2aMaps.stringList(contribution.get("capabilityTags")).stream())
                .distinct()
                .toList();
    }

    private static Map<String, Object> contribution(Map<String, Object> group) {
        Object value = group.get("contribution");
        if (value instanceof Map<?, ?> map) {
            return WayangA2aMaps.copyMap(map);
        }
        return Map.of();
    }

    private static List<String> stringList(Map<String, Object> values, String key) {
        return values.containsKey(key) ? WayangA2aMaps.stringList(values.get(key)) : null;
    }
}

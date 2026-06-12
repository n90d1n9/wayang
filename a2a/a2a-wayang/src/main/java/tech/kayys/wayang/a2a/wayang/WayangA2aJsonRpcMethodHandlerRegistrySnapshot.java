package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bool;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.text;

/**
 * JSON-ready snapshot of the JSON-RPC method handler registry assembly.
 */
public record WayangA2aJsonRpcMethodHandlerRegistrySnapshot(
        boolean reported,
        List<Map<String, Object>> groups,
        List<String> providerIds,
        List<String> moduleIds,
        List<String> capabilityTags,
        String overridePolicy,
        List<Map<String, Object>> overrides) {

    public WayangA2aJsonRpcMethodHandlerRegistrySnapshot {
        groups = copyObjects(groups);
        WayangA2aJsonRpcMethodHandlerRegistryProviderSummary providerSummary =
                WayangA2aJsonRpcMethodHandlerRegistryProviderSummary.from(
                        groups,
                        providerIds,
                        moduleIds,
                        capabilityTags);
        providerIds = providerSummary.providerIds();
        moduleIds = providerSummary.moduleIds();
        capabilityTags = providerSummary.capabilityTags();
        overridePolicy = WayangA2aMaps.optional(overridePolicy);
        overrides = copyObjects(overrides);
    }

    public WayangA2aJsonRpcMethodHandlerRegistrySnapshot(
            boolean reported,
            List<Map<String, Object>> groups,
            String overridePolicy,
            List<Map<String, Object>> overrides) {
        this(reported, groups, null, null, null, overridePolicy, overrides);
    }

    static WayangA2aJsonRpcMethodHandlerRegistrySnapshot from(
            WayangA2aJsonRpcMethodHandlerRegistry registry) {
        if (registry == null) {
            return empty();
        }
        return new WayangA2aJsonRpcMethodHandlerRegistrySnapshot(
                true,
                registry.groups().stream()
                        .map(WayangA2aJsonRpcMethodHandlerRegistrySnapshot::group)
                        .toList(),
                null,
                null,
                null,
                registry.overridePolicy().name(),
                registry.overrideMaps());
    }

    static WayangA2aJsonRpcMethodHandlerRegistrySnapshot fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        if (copy.isEmpty()) {
            return empty();
        }
        List<Map<String, Object>> groups = WayangA2aMaps.objectList(copy.get("groups"));
        WayangA2aJsonRpcMethodHandlerRegistryProviderSummary providerSummary =
                providerSummary(copy, groups);
        return new WayangA2aJsonRpcMethodHandlerRegistrySnapshot(
                bool(copy.get("reported"), reported(copy)),
                groups,
                providerSummary.providerIds(),
                providerSummary.moduleIds(),
                providerSummary.capabilityTags(),
                text(copy.get("overridePolicy"), ""),
                WayangA2aMaps.objectList(copy.get("overrides")));
    }

    private static WayangA2aJsonRpcMethodHandlerRegistrySnapshot empty() {
        return new WayangA2aJsonRpcMethodHandlerRegistrySnapshot(false, List.of(), null, List.of());
    }

    public int groupCount() {
        return groups.size();
    }

    public int providerCount() {
        return providerIds.size();
    }

    public int overrideCount() {
        return overrides.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("reported", reported);
        values.put("groupCount", groupCount());
        values.put("groups", groups);
        values.putAll(providerSummary().toMap());
        if (overridePolicy != null) {
            values.put("overridePolicy", overridePolicy);
        }
        values.put("overrideCount", overrideCount());
        values.put("overrides", overrides);
        return WayangA2aMaps.copyMap(values);
    }

    private static Map<String, Object> group(WayangA2aJsonRpcMethodHandlerGroup group) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", group.name());
        values.put("methodCount", group.handlers().size());
        values.put("methods", List.copyOf(group.handlers().keySet()));
        values.put("contribution", group.contribution().toMap());
        return WayangA2aMaps.copyMap(values);
    }

    private static List<Map<String, Object>> copyObjects(List<Map<String, Object>> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(WayangA2aMaps::copyMap)
                .toList();
    }

    private WayangA2aJsonRpcMethodHandlerRegistryProviderSummary providerSummary() {
        return new WayangA2aJsonRpcMethodHandlerRegistryProviderSummary(
                providerIds,
                moduleIds,
                capabilityTags);
    }

    private static WayangA2aJsonRpcMethodHandlerRegistryProviderSummary providerSummary(
            Map<String, Object> values,
            List<Map<String, Object>> groups) {
        return WayangA2aJsonRpcMethodHandlerRegistryProviderSummary.fromMap(values, groups);
    }

    private static boolean reported(Map<String, Object> values) {
        return values.containsKey("groups")
                || WayangA2aJsonRpcMethodHandlerRegistryProviderSummary.reported(values)
                || values.containsKey("overridePolicy")
                || values.containsKey("overrides")
                || values.containsKey("groupCount")
                || values.containsKey("overrideCount");
    }
}

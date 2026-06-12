package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WayangA2aJsonRpcMethodRegistryTestFixtures {

    static final String GROUP_TASK = "task";
    static final String GROUP_EXTENSION = "extension";
    static final String PROVIDER_TASK = "test.registry.task";
    static final String MODULE_TEST = "test-registry";
    static final String OVERRIDE_POLICY_ALLOW_REPLACE =
            WayangA2aJsonRpcMethodHandlerOverridePolicy.ALLOW_REPLACE.name();

    private WayangA2aJsonRpcMethodRegistryTestFixtures() {
    }

    static WayangA2aJsonRpcMethodHandlerRegistrySnapshot taskRegistrySnapshot() {
        return WayangA2aJsonRpcMethodHandlerRegistrySnapshot.fromMap(taskRegistryMap());
    }

    static Map<String, Object> taskRegistryMap() {
        return registryMap(List.of(taskGroupMap()), List.of());
    }

    static Map<String, Object> taskRegistryMapWithOverride() {
        return registryMap(List.of(taskGroupMap()), List.of(taskOverrideMap()));
    }

    static Map<String, Object> flattenedTaskRegistryProbeMapWithPaddedPolicy() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("methodRegistryReported", true);
        values.put("methodRegistryGroupCount", 1);
        values.put("methodRegistryGroups", List.of(taskGroupMap()));
        values.put("methodRegistryProviderCount", 1);
        values.put("methodRegistryProviderIds", List.of(PROVIDER_TASK));
        values.put("methodRegistryModuleIds", List.of(MODULE_TEST));
        values.put("methodRegistryCapabilityTags", List.of("test", "task"));
        values.put("methodRegistryOverridePolicy", " " + OVERRIDE_POLICY_ALLOW_REPLACE + " ");
        values.put("methodRegistryOverrideCount", 0);
        values.put("methodRegistryOverrides", List.of());
        return WayangA2aMaps.copyMap(values);
    }

    static Map<String, Object> taskGroupMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", GROUP_TASK);
        values.put("methodCount", 1);
        values.put("methods", List.of(WayangA2aJsonRpcMethods.GET_TASK));
        values.put("contribution", Map.of(
                "providerId", PROVIDER_TASK,
                "moduleId", MODULE_TEST,
                "capabilityTags", List.of("test", "task"),
                "priority", 0));
        return WayangA2aMaps.copyMap(values);
    }

    static Map<String, Object> taskOverrideMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("method", WayangA2aJsonRpcMethods.GET_TASK);
        values.put("originalGroup", GROUP_TASK);
        values.put("replacementGroup", GROUP_EXTENSION);
        return WayangA2aMaps.copyMap(values);
    }

    private static Map<String, Object> registryMap(
            List<Map<String, Object>> groups,
            List<Map<String, Object>> overrides) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("reported", true);
        values.put("groupCount", groups.size());
        values.put("groups", List.copyOf(groups));
        values.put("overridePolicy", OVERRIDE_POLICY_ALLOW_REPLACE);
        values.put("overrideCount", overrides.size());
        values.put("overrides", List.copyOf(overrides));
        return WayangA2aMaps.copyMap(values);
    }
}

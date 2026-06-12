package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata describing the source of a JSON-RPC method handler group.
 */
record WayangA2aJsonRpcMethodHandlerContribution(
        String providerId,
        String moduleId,
        List<String> capabilityTags,
        int priority) {

    WayangA2aJsonRpcMethodHandlerContribution {
        providerId = WayangA2aMaps.required(providerId, "providerId");
        String resolvedModuleId = WayangA2aMaps.optional(moduleId);
        moduleId = resolvedModuleId == null ? "" : resolvedModuleId;
        capabilityTags = WayangA2aMaps.stringList(capabilityTags);
        priority = Math.max(0, priority);
    }

    static WayangA2aJsonRpcMethodHandlerContribution forGroup(String groupName) {
        String resolved = WayangA2aMaps.required(groupName, "groupName");
        return new WayangA2aJsonRpcMethodHandlerContribution(resolved, "", List.of(), 0);
    }

    static WayangA2aJsonRpcMethodHandlerContribution core(
            String providerId,
            List<String> capabilityTags) {
        return new WayangA2aJsonRpcMethodHandlerContribution(
                providerId,
                WayangA2aJsonRpcCoreMethodHandlerContributions.MODULE_ID,
                capabilityTags,
                0);
    }

    Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("providerId", providerId);
        if (!moduleId.isBlank()) {
            values.put("moduleId", moduleId);
        }
        values.put("capabilityTags", capabilityTags);
        values.put("priority", priority);
        return WayangA2aMaps.copyMap(values);
    }
}

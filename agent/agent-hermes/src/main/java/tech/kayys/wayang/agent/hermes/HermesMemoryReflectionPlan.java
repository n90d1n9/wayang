package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Advisory memory consolidation contract for Hermes requests.
 */
public record HermesMemoryReflectionPlan(
        boolean memoryEnabled,
        boolean requested,
        boolean reflect,
        String scope,
        String cadence,
        String priority,
        String source,
        String reason) {

    public HermesMemoryReflectionPlan {
        scope = HermesText.trimOr(scope, "session");
        cadence = HermesText.trimOr(cadence, reflect ? "post-run" : "none");
        priority = HermesText.trimOr(priority, "normal");
        source = HermesText.trimOr(source, "none");
        reason = HermesText.trimOr(reason, "no memory reflection requested");
    }

    public boolean active() {
        return memoryEnabled && reflect;
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("memoryEnabled", memoryEnabled);
        metadata.put("requested", requested);
        metadata.put("reflect", reflect);
        metadata.put("active", active());
        metadata.put("scope", scope);
        metadata.put("cadence", cadence);
        metadata.put("priority", priority);
        metadata.put("source", source);
        metadata.put("reason", reason);
        return Map.copyOf(metadata);
    }
}

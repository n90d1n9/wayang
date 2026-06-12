package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single read-only remediation recommendation for learned-skill lineage.
 */
public record HermesSkillLineageRemediationAction(
        String action,
        String severity,
        boolean required,
        boolean automatic,
        String targetType,
        String target,
        String reason,
        Map<String, Object> metadata) {

    public HermesSkillLineageRemediationAction {
        action = HermesText.oneLineOr(action, "review-lineage");
        severity = HermesText.oneLineOr(severity, required ? "warning" : "info");
        targetType = HermesText.oneLineOr(targetType, "catalog");
        target = HermesText.oneLineOr(target, "");
        reason = HermesText.oneLineOr(reason, "lineage review recommended");
        metadata = HermesMetadata.copy(metadata);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("action", action);
        values.put("severity", severity);
        values.put("required", required);
        values.put("automatic", automatic);
        values.put("targetType", targetType);
        values.put("target", target);
        values.put("reason", reason);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }
}

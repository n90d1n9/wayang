package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Backend-neutral consistency issue for learned-skill storage.
 */
public record HermesSkillStoreConsistencyIssue(
        String issueType,
        String severity,
        boolean attentionRequired,
        String targetType,
        String target,
        String reason,
        Map<String, Object> metadata) {

    public HermesSkillStoreConsistencyIssue {
        issueType = HermesText.oneLineOr(issueType, "review-skill-store-consistency");
        severity = HermesText.oneLineOr(severity, attentionRequired ? "warning" : "info");
        targetType = HermesText.oneLineOr(targetType, "catalog");
        target = HermesText.oneLineOr(target, "");
        reason = HermesText.oneLineOr(reason, "learned-skill storage consistency review recommended");
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesSkillStoreConsistencyIssue from(HermesSkillLineageRemediationAction action) {
        if (action == null) {
            return new HermesSkillStoreConsistencyIssue(
                    "review-skill-store-consistency",
                    "info",
                    false,
                    "catalog",
                    "learned-skills",
                    "learned-skill storage consistency review recommended",
                    Map.of());
        }
        Map<String, Object> metadata = new LinkedHashMap<>(action.metadata());
        metadata.put("remediationAction", action.action());
        metadata.put("automatic", action.automatic());
        return new HermesSkillStoreConsistencyIssue(
                action.action(),
                action.severity(),
                action.required(),
                action.targetType(),
                action.target(),
                action.reason(),
                metadata);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("issueType", issueType);
        values.put("severity", severity);
        values.put("attentionRequired", attentionRequired);
        values.put("targetType", targetType);
        values.put("target", target);
        values.put("reason", reason);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }
}

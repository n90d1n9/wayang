package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structured validation issue for a learned-skill persistence route or resource.
 */
public record HermesSkillPersistenceValidationIssue(
        String role,
        String store,
        String storeType,
        String severity,
        String reason,
        String recommendedAction) {

    public HermesSkillPersistenceValidationIssue {
        role = HermesSkillPersistenceRouteRoles.normalize(role);
        store = HermesDirectiveSupport.clean(store, "unknown");
        storeType = HermesDirectiveSupport.clean(storeType, HermesSkillPersistenceStoreClassifier.storeType(store));
        severity = HermesDirectiveSupport.clean(severity, "error");
        reason = HermesDirectiveSupport.clean(reason, "learned-skill persistence validation failed");
        recommendedAction = HermesDirectiveSupport.clean(recommendedAction, "Review Hermes persistence hints");
    }

    static HermesSkillPersistenceValidationIssue error(
            String role,
            String store,
            String storeType,
            String reason,
            String recommendedAction) {
        return new HermesSkillPersistenceValidationIssue(
                role,
                store,
                storeType,
                "error",
                reason,
                recommendedAction);
    }

    public boolean error() {
        return "error".equals(severity);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("role", role);
        values.put("store", store);
        values.put("storeType", storeType);
        values.put("severity", severity);
        values.put("reason", reason);
        values.put("recommendedAction", recommendedAction);
        return Map.copyOf(values);
    }
}

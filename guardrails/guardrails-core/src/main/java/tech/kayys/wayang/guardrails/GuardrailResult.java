package tech.kayys.wayang.guardrails;

import tech.kayys.wayang.guardrails.plugin.api.Finding;
import java.util.List;
import java.util.Map;

public record GuardrailResult(
        boolean allowed,
        String reason,
        List<String> triggeredPolicies,
        List<Finding> findings,
        Map<String, Object> redactedContent) {

    public static GuardrailResult success() {
        return new GuardrailResult(true, null, List.of(), List.of(), Map.of());
    }

    public static GuardrailResult failure(String reason, String policyId) {
        return new GuardrailResult(false, reason, List.of(policyId), List.of(), Map.of());
    }

    public static GuardrailResult failure(String reason, List<String> detectorIds) {
        return new GuardrailResult(false, reason, detectorIds, List.of(), Map.of());
    }

    public static GuardrailResult failure(String reason, List<String> detectorIds, List<Finding> findings) {
        return new GuardrailResult(false, reason, detectorIds, findings, Map.of());
    }

    public GuardrailResult withRedactedContent(Map<String, Object> content) {
        return new GuardrailResult(allowed, reason, triggeredPolicies, findings, content);
    }

    public GuardrailResult withFindings(List<Finding> findings) {
        return new GuardrailResult(allowed, reason, triggeredPolicies, findings, redactedContent);
    }
}

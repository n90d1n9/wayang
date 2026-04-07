package tech.kayys.wayang.guardrails.policy;

public record PolicyEvaluationResult(
        boolean allowed,
        String reason,
        String policyId) {
    public static PolicyEvaluationResult success() {
        return new PolicyEvaluationResult(true, null, null);
    }

    public static PolicyEvaluationResult denied(String reason, String policyId) {
        return new PolicyEvaluationResult(false, reason, policyId);
    }
}
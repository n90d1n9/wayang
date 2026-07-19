package tech.kayys.wayang.guardrails.policy;

import java.util.List;

public record PolicyResult(List<PolicyViolation> violations) {
    public List<PolicyViolation> getViolations() {
        return violations;
    }
}

package tech.kayys.wayang.guardrails;

import tech.kayys.wayang.guardrails.plugin.api.CheckPhase;
import tech.kayys.wayang.guardrails.policy.PolicySeverity;

public record GuardrailPolicy(
                String id,
                String name,
                String expression,
                String denyMessage,
                PolicySeverity severity,
                CheckPhase phase) {
}

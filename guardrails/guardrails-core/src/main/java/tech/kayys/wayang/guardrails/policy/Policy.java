package tech.kayys.wayang.guardrails.policy;

import tech.kayys.wayang.guardrails.plugin.api.CheckPhase;

public record Policy(
                String id,
                String name,
                String expression,
                String denyMessage,
                PolicySeverity severity,
                CheckPhase phase) {
}
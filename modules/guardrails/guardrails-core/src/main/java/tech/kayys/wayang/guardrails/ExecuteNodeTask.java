package tech.kayys.wayang.guardrails;

import java.util.Map;

public record ExecuteNodeTask(
        String nodeId,
        String nodeType,
        String tenantId,
        Map<String, Object> inputs,
        Map<String, Object> config) {
}

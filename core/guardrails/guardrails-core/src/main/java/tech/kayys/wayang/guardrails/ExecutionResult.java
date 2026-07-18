package tech.kayys.wayang.guardrails;

import java.util.Map;

public record ExecutionResult(
        Map<String, Object> outputs,
        Map<String, Object> metadata) {
}

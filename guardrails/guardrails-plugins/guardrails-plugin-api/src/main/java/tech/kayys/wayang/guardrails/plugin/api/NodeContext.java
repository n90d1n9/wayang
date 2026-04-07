package tech.kayys.wayang.guardrails.plugin.api;

import java.util.Map;

/**
 * Context for node execution, providing tenant and metadata information.
 */
public record NodeContext(String tenantId, Map<String, Object> inputs, NodeMetadata metadata) {
    public record NodeMetadata(String userId, Map<String, Object> additionalInfo) {
    }
}

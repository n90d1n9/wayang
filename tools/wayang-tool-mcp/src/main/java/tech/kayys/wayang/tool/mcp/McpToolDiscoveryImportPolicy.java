package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class McpToolDiscoveryImportPolicy {

    static final String CONTEXT_ALLOW_RETIRED_REACTIVATION = "allowRetiredReactivation";
    static final String METADATA_SKIPPED_RETIRED_TOOL_IDS = "skippedRetiredToolIds";

    private McpToolDiscoveryImportPolicy() {
    }

    static boolean shouldMarkStale(
            tech.kayys.wayang.tool.entity.McpTool existingTool,
            McpToolDiscoveryImportRequest request,
            Set<String> activeToolIds) {
        if (existingTool == null
                || !existingTool.isEnabled()
                || activeToolIds.contains(existingTool.getToolId())
                || !McpToolLifecycle.hasValue(existingTool.getCapabilities(), McpToolLifecycle.MCP_TAG)) {
            return false;
        }
        String serverName = request.serverName();
        if (serverName != null && !serverName.isBlank()) {
            return McpToolLifecycle.belongsToServer(existingTool, serverName);
        }
        String requestEndpoint = McpToolDiscoveryImportMapper.endpoint(request);
        return requestEndpoint != null
                && requestEndpoint.equals(McpToolRegistryMetadata.registryEndpoint(existingTool));
    }

    static void markStale(tech.kayys.wayang.tool.entity.McpTool tool) {
        tool.setEnabled(false);
        tool.setUpdatedAt(Instant.now());
        tool.setTags(McpToolLifecycle.withValue(tool.getTags(), McpToolLifecycle.STALE_TAG));
        tool.setCapabilities(McpToolLifecycle.withValue(tool.getCapabilities(), McpToolLifecycle.STALE_TAG));
    }

    static boolean isReactivationCandidate(tech.kayys.wayang.tool.entity.McpTool tool) {
        return tool != null && !tool.isEnabled() && McpToolLifecycle.isStale(tool);
    }

    static boolean shouldSkipRetired(
            tech.kayys.wayang.tool.entity.McpTool existing,
            McpToolDiscoveryImportRequest request) {
        return McpToolLifecycle.isRetired(existing) && !allowRetiredReactivation(request);
    }

    static boolean allowRetiredReactivation(McpToolDiscoveryImportRequest request) {
        Object value = request.context().get(CONTEXT_ALLOW_RETIRED_REACTIVATION);
        if (value == null) {
            value = McpToolInvocationFields.customData(request.context()).get(CONTEXT_ALLOW_RETIRED_REACTIVATION);
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        return value instanceof String text && Boolean.parseBoolean(text);
    }

    static Map<String, Object> metadataWithSkippedRetired(
            Map<String, Object> metadata,
            List<String> skippedRetiredToolIds) {
        if (skippedRetiredToolIds.isEmpty()) {
            return metadata;
        }
        Map<String, Object> copy = new LinkedHashMap<>(McpMaps.copy(metadata));
        copy.put(METADATA_SKIPPED_RETIRED_TOOL_IDS, skippedRetiredToolIds);
        return Map.copyOf(copy);
    }
}

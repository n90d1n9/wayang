package tech.kayys.wayang.tool.dto;

import java.util.List;

public record UnifiedRegistrySnapshotResponse(
        List<ToolMetadataResponse> tools,
        List<McpServerRegistryResponse> mcpServers) {
}


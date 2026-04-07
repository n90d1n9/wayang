package tech.kayys.wayang.tool.dto;

public record McpRegistryImportRequest(
        String sourceType,
        String source,
        String serverName) {
}

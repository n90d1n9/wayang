package tech.kayys.wayang.tool.dto;

import java.util.List;

public record McpRegistryImportResponse(
        int importedCount,
        List<String> serverNames) {
}


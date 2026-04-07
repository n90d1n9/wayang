package tech.kayys.wayang.tool.dto;

import java.util.List;

public record UnifiedRegistryImportResponse(
        String detectedFormat,
        int toolsGenerated,
        List<String> toolIds,
        int mcpServersImported,
        List<String> mcpServerNames,
        List<String> warnings) {
}


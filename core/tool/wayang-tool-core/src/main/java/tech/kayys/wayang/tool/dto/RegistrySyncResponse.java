package tech.kayys.wayang.tool.dto;

import java.util.List;

public record RegistrySyncResponse(
        int openApiSourcesScanned,
        int openApiSourcesUpdated,
        int openApiToolsUpserted,
        int mcpSourcesScanned,
        int mcpServersImported,
        List<String> warnings) {
}


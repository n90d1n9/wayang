package tech.kayys.wayang.tool.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record UnifiedRegistryImportRequest(
        @NotBlank String sourceType,
        @NotBlank String source,
        String format,
        String namespace,
        String authProfileId,
        Map<String, Object> guardrailsConfig,
        String serverName) {
}


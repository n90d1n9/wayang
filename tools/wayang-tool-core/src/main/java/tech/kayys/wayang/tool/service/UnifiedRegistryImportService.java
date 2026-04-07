package tech.kayys.wayang.tool.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.dto.GenerateToolsRequest;
import tech.kayys.wayang.tool.dto.McpRegistryImportRequest;
import tech.kayys.wayang.tool.dto.SourceType;
import tech.kayys.wayang.tool.dto.ToolGenerationResult;
import tech.kayys.wayang.tool.dto.UnifiedRegistryImportRequest;
import tech.kayys.wayang.tool.dto.UnifiedRegistryImportResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class UnifiedRegistryImportService {

    @Inject
    ToolGenerationService toolGenerationService;

    @Inject
    McpRegistryService mcpRegistryService;

    @Inject
    EditionModeService editionModeService;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public Uni<UnifiedRegistryImportResponse> importSource(String requestId, String userId, UnifiedRegistryImportRequest request) {
        String explicitFormat = request.format() == null ? null : request.format().trim().toUpperCase(Locale.ROOT);
        String detected = explicitFormat;

        if (detected == null || detected.isBlank()) {
            try {
                String content = loadContent(request.sourceType(), request.source());
                detected = detectFormat(content);
            } catch (Exception e) {
                return Uni.createFrom().failure(new IllegalArgumentException("Unable to detect source format: " + e.getMessage(), e));
            }
        }

        if ("MCP".equals(detected) || "MCP_JSON".equals(detected)) {
            if (!editionModeService.supportsMcpRegistryDatabase()) {
                return Uni.createFrom().failure(new jakarta.ws.rs.ForbiddenException(
                        "MCP registry database mode is enterprise-only. Use file-based MCP config in community mode."));
            }
            McpRegistryImportRequest mcpRequest = new McpRegistryImportRequest(
                    request.sourceType(),
                    request.source(),
                    request.serverName());
            return mcpRegistryService.importFromJson(requestId, mcpRequest)
                    .map(result -> new UnifiedRegistryImportResponse(
                            "MCP_JSON",
                            0,
                            List.of(),
                            result.importedCount(),
                            result.serverNames(),
                            List.of()));
        }

        if ("OPENAPI".equals(detected) || "OAS".equals(detected) || "OPENAPI_OAS".equals(detected)) {
            if (request.namespace() == null || request.namespace().isBlank()) {
                return Uni.createFrom().failure(new IllegalArgumentException("`namespace` is required for OpenAPI/OAS import."));
            }
            SourceType sourceType = SourceType.valueOf(request.sourceType().trim().toUpperCase(Locale.ROOT));
            GenerateToolsRequest generateRequest = new GenerateToolsRequest(
                    requestId,
                    request.namespace().trim(),
                    sourceType,
                    request.source(),
                    request.authProfileId(),
                    userId,
                    request.guardrailsConfig() != null ? request.guardrailsConfig() : Map.of());

            return toolGenerationService.generateTools(generateRequest)
                    .map(result -> toOpenApiResponse(result));
        }

        return Uni.createFrom().failure(new IllegalArgumentException("Unsupported format: " + detected));
    }

    private UnifiedRegistryImportResponse toOpenApiResponse(ToolGenerationResult result) {
        return new UnifiedRegistryImportResponse(
                "OPENAPI_OAS",
                result.toolsGenerated(),
                result.toolIds(),
                0,
                List.of(),
                result.warnings() != null ? result.warnings() : List.of());
    }

    private String detectFormat(String content) throws IOException {
        JsonNode root = tryParse(content);
        if (root == null || !root.isObject()) {
            throw new IOException("Source is not a JSON/YAML object.");
        }
        if (root.has("mcpServers")) {
            return "MCP_JSON";
        }
        if (root.has("openapi") || root.has("swagger") || root.has("paths")) {
            return "OPENAPI_OAS";
        }
        throw new IOException("Could not detect OpenAPI/OAS or MCP JSON format.");
    }

    private JsonNode tryParse(String content) {
        try {
            return jsonMapper.readTree(content);
        } catch (Exception ignored) {
        }
        try {
            return yamlMapper.readTree(content);
        } catch (Exception ignored) {
        }
        return null;
    }

    private String loadContent(String sourceTypeRaw, String source) throws IOException, InterruptedException {
        String sourceType = sourceTypeRaw.trim().toUpperCase(Locale.ROOT);
        return switch (sourceType) {
            case "URL", "OPENAPI_3_URL", "SWAGGER_2_URL" -> fetch(source);
            case "FILE", "OPENAPI_3_FILE", "SWAGGER_2_FILE" -> Files.readString(Path.of(source), StandardCharsets.UTF_8);
            case "RAW", "OPENAPI_3_RAW", "SWAGGER_2_RAW" -> source;
            default -> throw new IllegalArgumentException("Unsupported sourceType: " + sourceType);
        };
    }

    private String fetch(String source) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(source))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }
}

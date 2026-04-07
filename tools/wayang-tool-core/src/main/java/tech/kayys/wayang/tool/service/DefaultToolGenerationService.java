package tech.kayys.wayang.tool.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.dto.CapabilityLevel;
import tech.kayys.wayang.tool.dto.GenerateToolsRequest;
import tech.kayys.wayang.tool.dto.HttpMethod;
import tech.kayys.wayang.tool.dto.ParameterLocation;
import tech.kayys.wayang.tool.dto.ParameterMapping;
import tech.kayys.wayang.tool.dto.SourceType;
import tech.kayys.wayang.tool.dto.ToolGenerationResult;
import tech.kayys.wayang.tool.dto.ToolType;
import tech.kayys.wayang.tool.entity.HttpExecutionConfig;
import tech.kayys.wayang.tool.entity.McpTool;
import tech.kayys.wayang.tool.entity.OpenApiSource;
import tech.kayys.wayang.tool.entity.ToolGuardrails;
import tech.kayys.wayang.tool.entity.ToolMetrics;
import tech.kayys.wayang.tool.repository.OpenApiSourceRepository;
import tech.kayys.wayang.tool.repository.ToolRepository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class DefaultToolGenerationService implements ToolGenerationService {

    private static final Set<String> SUPPORTED_METHODS = Set.of("get", "post", "put", "patch", "delete", "head", "options");

    @Inject
    OpenApiSourceRepository openApiSourceRepository;

    @Inject
    ToolRepository toolRepository;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Override
    public Uni<ToolGenerationResult> generateTools(GenerateToolsRequest request) {
        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
            try {
                String specContent = resolveSpecContent(request.sourceType(), request.source());
                JsonNode specRoot = parseSpec(specContent);

                OpenApiSource source = buildSource(request, specContent, specRoot);
                return openApiSourceRepository.save(source)
                        .flatMap(savedSource -> {
                            List<McpTool> tools = generateToolsFromSpec(savedSource, request, specRoot);
                            if (tools.isEmpty()) {
                                return Uni.createFrom().item(new ToolGenerationResult(
                                        savedSource.getSourceId(),
                                        request.namespace(),
                                        0,
                                        List.of(),
                                        List.of("No operations found in specification")));
                            }

                            return persistTools(tools)
                                    .flatMap(toolIds -> {
                                        savedSource.setToolsGenerated(toolIds.size());
                                        savedSource.setLastSyncAt(Instant.now());
                                        savedSource.setUpdatedAt(Instant.now());
                                        return openApiSourceRepository.save(savedSource)
                                                .replaceWith(new ToolGenerationResult(
                                                        savedSource.getSourceId(),
                                                        request.namespace(),
                                                        toolIds.size(),
                                                        toolIds,
                                                        List.of()));
                                    });
                        });
            } catch (Exception e) {
                return Uni.createFrom().failure(new IllegalArgumentException("Failed to generate tools from OpenAPI source: " + e.getMessage(), e));
            }
        });
    }

    public Uni<ToolGenerationResult> syncSource(OpenApiSource source, String userId) {
        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
            try {
                String specContent = resolveSpecContent(source.getSourceType(), source.getSourceLocation());
                String newHash = sha256(specContent);
                if (newHash.equals(source.getSpecHash())) {
                    return Uni.createFrom().item(new ToolGenerationResult(
                            source.getSourceId(),
                            source.getNamespace(),
                            0,
                            List.of(),
                            List.of("No changes detected for source " + source.getSourceId())));
                }

                JsonNode specRoot = parseSpec(specContent);
                source.setSpecContent(specContent);
                source.setSpecVersion(detectSpecVersion(specRoot));
                source.setSpecHash(newHash);
                source.setUpdatedAt(Instant.now());
                source.setLastSyncAt(Instant.now());

                GenerateToolsRequest request = new GenerateToolsRequest(
                        source.getRequestId(),
                        source.getNamespace(),
                        source.getSourceType(),
                        source.getSourceLocation(),
                        source.getDefaultAuthProfileId(),
                        userId,
                        Map.of());

                List<McpTool> tools = generateToolsFromSpec(source, request, specRoot);
                return upsertTools(tools)
                        .flatMap(toolIds -> {
                            source.setToolsGenerated(toolIds.size());
                            return openApiSourceRepository.save(source)
                                    .replaceWith(new ToolGenerationResult(
                                            source.getSourceId(),
                                            source.getNamespace(),
                                            toolIds.size(),
                                            toolIds,
                                            List.of()));
                        });
            } catch (Exception e) {
                return Uni.createFrom().failure(new IllegalArgumentException("Failed to sync source " + source.getSourceId() + ": " + e.getMessage(), e));
            }
        });
    }

    private Uni<List<String>> persistTools(List<McpTool> tools) {
        Uni<List<String>> chain = Uni.createFrom().item(new ArrayList<>());
        for (McpTool tool : tools) {
            chain = chain.flatMap(ids -> toolRepository.save(tool).map(saved -> {
                ids.add(saved.getToolId());
                return ids;
            }));
        }
        return chain;
    }

    private Uni<List<String>> upsertTools(List<McpTool> tools) {
        Uni<List<String>> chain = Uni.createFrom().item(new ArrayList<>());
        for (McpTool tool : tools) {
            chain = chain.flatMap(ids -> toolRepository.findByRequestIdAndToolId(tool.getRequestId(), tool.getToolId())
                    .flatMap(existing -> {
                        if (existing == null) {
                            return toolRepository.save(tool).map(saved -> saved.getToolId());
                        }
                        copyToolState(existing, tool);
                        return Uni.createFrom().item(existing.getToolId());
                    })
                    .map(toolId -> {
                        ids.add(toolId);
                        return ids;
                    }));
        }
        return chain;
    }

    private void copyToolState(McpTool target, McpTool source) {
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setVersion(source.getVersion());
        target.setToolType(source.getToolType());
        target.setCapabilityLevel(source.getCapabilityLevel());
        target.setInputSchema(source.getInputSchema());
        target.setOutputSchema(source.getOutputSchema());
        target.setExecutionConfig(source.getExecutionConfig());
        target.setAuthProfileId(source.getAuthProfileId());
        target.setGuardrails(source.getGuardrails());
        target.setEnabled(source.isEnabled());
        target.setReadOnly(source.isReadOnly());
        target.setRequiresApproval(source.isRequiresApproval());
        target.setTags(source.getTags());
        target.setCapabilities(source.getCapabilities());
        target.setUpdatedAt(Instant.now());
        target.setSource(source.getSource());
        target.setOperationId(source.getOperationId());
    }

    private OpenApiSource buildSource(GenerateToolsRequest request, String specContent, JsonNode specRoot) {
        OpenApiSource source = new OpenApiSource();
        source.setRequestId(request.requestId());
        source.setNamespace(request.namespace());
        source.setDisplayName(request.namespace());
        source.setSourceType(request.sourceType());
        source.setSourceLocation(request.source());
        source.setSpecContent(specContent);
        source.setSpecVersion(detectSpecVersion(specRoot));
        source.setSpecHash(sha256(specContent));
        source.setDefaultAuthProfileId(request.authProfileId());
        source.setCreatedBy(request.userId());
        source.setCreatedAt(Instant.now());
        source.setUpdatedAt(Instant.now());
        source.setEnabled(true);
        return source;
    }

    private List<McpTool> generateToolsFromSpec(OpenApiSource source, GenerateToolsRequest request, JsonNode specRoot) {
        List<McpTool> tools = new ArrayList<>();
        JsonNode paths = specRoot.path("paths");
        if (!paths.isObject()) {
            return tools;
        }

        paths.fields().forEachRemaining(pathEntry -> {
            String path = pathEntry.getKey();
            JsonNode pathNode = pathEntry.getValue();
            if (!pathNode.isObject()) {
                return;
            }
            pathNode.fields().forEachRemaining(methodEntry -> {
                String methodRaw = methodEntry.getKey().toLowerCase();
                if (!SUPPORTED_METHODS.contains(methodRaw)) {
                    return;
                }

                JsonNode operation = methodEntry.getValue();
                String operationId = extractOperationId(operation, methodRaw, path);
                McpTool tool = new McpTool();
                tool.setToolId(request.namespace() + "." + operationId);
                tool.setRequestId(request.requestId());
                tool.setNamespace(request.namespace());
                tool.setName(operationId);
                tool.setDescription(extractDescription(operation));
                tool.setToolType(ToolType.HTTP);
                tool.setCapabilityLevel(toCapabilityLevel(methodRaw));
                tool.setInputSchema(buildInputSchema(operation));
                tool.setOutputSchema(buildOutputSchema(operation));
                tool.setExecutionConfig(buildExecutionConfig(specRoot, path, methodRaw, operation));
                tool.setAuthProfileId(request.authProfileId());
                tool.setGuardrails(buildGuardrails(request.guardrailsConfig()));
                tool.setEnabled(true);
                tool.setReadOnly("get".equals(methodRaw) || "head".equals(methodRaw) || "options".equals(methodRaw));
                tool.setRequiresApproval(toCapabilityLevel(methodRaw) == CapabilityLevel.DESTRUCTIVE);
                tool.setTags(defaultTags(methodRaw, path));
                tool.setCapabilities(defaultCapabilities(methodRaw));
                tool.setCreatedAt(Instant.now());
                tool.setUpdatedAt(Instant.now());
                tool.setCreatedBy(request.userId());
                tool.setSource(source);
                tool.setOperationId(operationId);
                tool.setMetrics(new ToolMetrics());
                tools.add(tool);
            });
        });

        return tools;
    }

    private HttpExecutionConfig buildExecutionConfig(JsonNode root, String path, String method, JsonNode operation) {
        HttpExecutionConfig config = new HttpExecutionConfig();
        config.setMethod(HttpMethod.valueOf(method.toUpperCase()));
        config.setBaseUrl(extractBaseUrl(root));
        config.setPath(path);
        config.setContentType("application/json");
        config.setAccept("application/json");

        List<ParameterMapping> mappings = new ArrayList<>();
        JsonNode params = operation.path("parameters");
        if (params.isArray()) {
            params.forEach(param -> {
                if (!param.isObject()) {
                    return;
                }
                ParameterMapping mapping = new ParameterMapping();
                mapping.setName(param.path("name").asText());
                mapping.setMappedName(param.path("name").asText());
                mapping.setRequired(param.path("required").asBoolean(false));
                mapping.setDescription(param.path("description").asText(null));
                mapping.setLocation(toParameterLocation(param.path("in").asText()));
                mappings.add(mapping);
            });
        }
        config.setParameters(mappings);
        config.setHeaders(new HashMap<>());
        return config;
    }

    private ToolGuardrails buildGuardrails(Map<String, Object> guardrailsConfig) {
        ToolGuardrails guardrails = new ToolGuardrails();
        if (guardrailsConfig == null || guardrailsConfig.isEmpty()) {
            return guardrails;
        }
        Object timeoutMs = guardrailsConfig.get("timeoutMs");
        if (timeoutMs instanceof Number n) {
            guardrails.setTimeoutMs(n.intValue());
        }
        Object maxRequestSizeKb = guardrailsConfig.get("maxRequestSizeKb");
        if (maxRequestSizeKb instanceof Number n) {
            guardrails.setMaxRequestSizeKb(n.intValue());
        }
        Object maxResponseSizeKb = guardrailsConfig.get("maxResponseSizeKb");
        if (maxResponseSizeKb instanceof Number n) {
            guardrails.setMaxResponseSizeKb(n.intValue());
        }
        Object rateLimitPerMinute = guardrailsConfig.get("rateLimitPerMinute");
        if (rateLimitPerMinute instanceof Number n) {
            guardrails.setRateLimitPerMinute(n.intValue());
        }
        return guardrails;
    }

    private Map<String, Object> buildInputSchema(JsonNode operation) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        JsonNode parameters = operation.path("parameters");
        if (parameters.isArray()) {
            parameters.forEach(param -> {
                String name = param.path("name").asText(null);
                if (name == null) {
                    return;
                }
                Map<String, Object> propSchema = new HashMap<>();
                propSchema.put("type", "string");
                properties.put(name, propSchema);
                if (param.path("required").asBoolean(false)) {
                    required.add(name);
                }
            });
        }

        JsonNode bodySchema = operation.path("requestBody").path("content").path("application/json").path("schema");
        if (bodySchema.isObject()) {
            properties.put("body", jsonMapper.convertValue(bodySchema, new TypeReference<Map<String, Object>>() {
            }));
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private Map<String, Object> buildOutputSchema(JsonNode operation) {
        JsonNode success = operation.path("responses").path("200").path("content").path("application/json").path("schema");
        if (!success.isObject()) {
            success = operation.path("responses").path("201").path("content").path("application/json").path("schema");
        }
        if (success.isObject()) {
            return jsonMapper.convertValue(success, new TypeReference<Map<String, Object>>() {
            });
        }
        return Map.of("type", "object");
    }

    private String resolveSpecContent(SourceType sourceType, String source) throws IOException, InterruptedException {
        return switch (sourceType) {
            case OPENAPI_3_URL, SWAGGER_2_URL, URL -> fetchUrl(source);
            case OPENAPI_3_FILE, SWAGGER_2_FILE, FILE -> Files.readString(Path.of(source), StandardCharsets.UTF_8);
            case OPENAPI_3_RAW, SWAGGER_2_RAW, RAW -> source;
            default -> throw new IllegalArgumentException("Unsupported source type for OpenAPI generation: " + sourceType);
        };
    }

    private String fetchUrl(String source) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(source))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Unable to fetch source URL. HTTP " + response.statusCode());
        }
        return response.body();
    }

    private JsonNode parseSpec(String specContent) throws JsonProcessingException {
        try {
            return jsonMapper.readTree(specContent);
        } catch (JsonProcessingException e) {
            return yamlMapper.readTree(specContent);
        }
    }

    private String detectSpecVersion(JsonNode root) {
        if (root.has("openapi")) {
            return root.path("openapi").asText();
        }
        if (root.has("swagger")) {
            return root.path("swagger").asText();
        }
        return "unknown";
    }

    private String extractBaseUrl(JsonNode root) {
        JsonNode servers = root.path("servers");
        if (servers.isArray() && !servers.isEmpty()) {
            return servers.get(0).path("url").asText("");
        }
        JsonNode host = root.path("host");
        if (!host.isMissingNode()) {
            String scheme = root.path("schemes").isArray() && !root.path("schemes").isEmpty()
                    ? root.path("schemes").get(0).asText("https")
                    : "https";
            String basePath = root.path("basePath").asText("");
            return scheme + "://" + host.asText() + basePath;
        }
        return "";
    }

    private String extractOperationId(JsonNode operation, String method, String path) {
        String explicit = operation.path("operationId").asText(null);
        if (explicit != null && !explicit.isBlank()) {
            return sanitize(explicit);
        }
        return sanitize(method + "_" + path.replace("/", "_").replace("{", "").replace("}", ""));
    }

    private String extractDescription(JsonNode operation) {
        String summary = operation.path("summary").asText(null);
        if (summary != null && !summary.isBlank()) {
            return summary;
        }
        String description = operation.path("description").asText(null);
        return description != null ? description : "";
    }

    private CapabilityLevel toCapabilityLevel(String method) {
        return switch (method.toLowerCase()) {
            case "get", "head", "options" -> CapabilityLevel.READ_ONLY;
            case "delete" -> CapabilityLevel.DESTRUCTIVE;
            case "post", "put", "patch" -> CapabilityLevel.STATE_CHANGING;
            default -> CapabilityLevel.UNKNOWN;
        };
    }

    private Set<String> defaultTags(String method, String path) {
        Set<String> tags = new HashSet<>();
        tags.add(method.toLowerCase());
        tags.add(path);
        return tags;
    }

    private Set<String> defaultCapabilities(String method) {
        return switch (method.toLowerCase()) {
            case "get", "head", "options" -> Set.of("read");
            case "delete" -> Set.of("write", "delete");
            default -> Set.of("write");
        };
    }

    private ParameterLocation toParameterLocation(String raw) {
        return switch (raw) {
            case "path" -> ParameterLocation.PATH;
            case "header" -> ParameterLocation.HEADER;
            case "query" -> ParameterLocation.QUERY;
            default -> ParameterLocation.BODY;
        };
    }

    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9_\\-.]", "_");
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}

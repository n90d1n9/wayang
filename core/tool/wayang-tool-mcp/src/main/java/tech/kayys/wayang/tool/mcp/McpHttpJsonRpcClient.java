package tech.kayys.wayang.tool.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class McpHttpJsonRpcClient {

    static final String CONTEXT_MCP_ENDPOINT = McpHttpTransportContext.CONTEXT_MCP_ENDPOINT;
    static final String CONTEXT_MCP_URL = McpHttpTransportContext.CONTEXT_MCP_URL;
    static final String CONTEXT_SERVER_URL = McpHttpTransportContext.CONTEXT_SERVER_URL;
    static final String CONTEXT_URL = McpHttpTransportContext.CONTEXT_URL;
    static final String CONTEXT_HEADERS = McpHttpTransportContext.CONTEXT_HEADERS;
    static final String CONTEXT_MCP_HEADERS = McpHttpTransportContext.CONTEXT_MCP_HEADERS;
    static final String CONTEXT_TIMEOUT_MS = McpHttpTransportContext.CONTEXT_TIMEOUT_MS;
    static final String CONTEXT_PROTOCOL_VERSION = McpHttpTransportContext.CONTEXT_PROTOCOL_VERSION;
    static final String DEFAULT_PROTOCOL_VERSION = McpHttpTransportContext.DEFAULT_PROTOCOL_VERSION;

    private static final String MCP_PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    McpHttpJsonRpcClient(HttpClient httpClient, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    Uni<McpHttpJsonRpcResponse> request(
            Map<String, Object> context,
            String method,
            Map<String, Object> params) {
        return send(context, McpJsonRpcProtocol.request(method, UUID.randomUUID().toString(), params));
    }

    Uni<McpHttpJsonRpcResponse> notification(
            Map<String, Object> context,
            String method,
            Map<String, Object> params) {
        return send(context, McpJsonRpcProtocol.notification(method, params));
    }

    String protocolVersion(Map<String, Object> context) {
        return McpHttpTransportContext.protocolVersion(context);
    }

    Optional<String> endpoint(Map<String, Object> context) {
        return McpHttpTransportContext.endpoint(context);
    }

    private Uni<McpHttpJsonRpcResponse> send(Map<String, Object> context, Map<String, Object> payload) {
        Instant startedAt = Instant.now();
        Optional<String> endpoint = endpoint(context);
        if (endpoint.isEmpty()) {
            return Uni.createFrom().failure(new IllegalArgumentException(
                    "MCP HTTP endpoint is required in invocation context"));
        }
        String protocolVersion = protocolVersion(context);

        HttpRequest request;
        try {
            request = buildRequest(context, endpoint.get(), protocolVersion, payload);
        } catch (RuntimeException | JsonProcessingException error) {
            return Uni.createFrom().failure(error);
        }

        return Uni.createFrom().completionStage(httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .map(response -> toResponse(
                        response,
                        startedAt,
                        McpJsonRpcProtocol.method(payload),
                        protocolVersion,
                        McpJsonRpcProtocol.id(payload)));
    }

    private HttpRequest buildRequest(
            Map<String, Object> context,
            String endpoint,
            String protocolVersion,
            Map<String, Object> payload) throws JsonProcessingException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(timeout(context))
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json")
                .header(MCP_PROTOCOL_VERSION_HEADER, protocolVersion)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)));

        McpHttpTransportContext.headers(context).forEach(builder::header);
        return builder.build();
    }

    private McpHttpJsonRpcResponse toResponse(
            HttpResponse<String> response,
            Instant startedAt,
            String method,
            String protocolVersion,
            Object requestId) {
        long durationMs = elapsedMs(startedAt);
        String responseContentType = response.headers().firstValue("Content-Type").orElse(null);
        Object responseId = null;
        try {
            Map<String, Object> body = parseBody(response.body());
            responseId = McpJsonRpcProtocol.id(body);
            if (requestId != null && httpOk(response.statusCode())) {
                body = McpJsonRpcProtocol.validateResponse(body, requestId);
            }
            return new McpHttpJsonRpcResponse(
                    response.statusCode(),
                    response.uri().toString(),
                    method,
                    protocolVersion,
                    responseContentType,
                    requestId,
                    responseId,
                    body,
                    durationMs);
        } catch (IOException | McpJsonRpcProtocolException error) {
            return new McpHttpJsonRpcResponse(
                    response.statusCode(),
                    response.uri().toString(),
                    method,
                    protocolVersion,
                    responseContentType,
                    requestId,
                    responseId,
                    McpJsonRpcProtocol.errorBody("Failed to parse MCP response: " + error.getMessage()),
                    durationMs,
                    McpFailureType.PARSE);
        }
    }

    private Map<String, Object> parseBody(String body) throws IOException {
        String json = McpServerSentEvents.extractData(body);
        if (json.isBlank()) {
            return Map.of();
        }
        return mapper.readValue(json, MAP_TYPE);
    }

    private Duration timeout(Map<String, Object> context) {
        return McpHttpTransportContext.timeout(context);
    }

    private boolean httpOk(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private long elapsedMs(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }
}

record McpHttpJsonRpcResponse(
        int httpStatus,
        String endpoint,
        String method,
        String protocolVersion,
        String responseContentType,
        Object requestId,
        Object responseId,
        Map<String, Object> body,
        long durationMs,
        McpFailureType failureType) {

    McpHttpJsonRpcResponse(
            int httpStatus,
            String endpoint,
            String method,
            String protocolVersion,
            String responseContentType,
            Object requestId,
            Object responseId,
            Map<String, Object> body,
            long durationMs) {
        this(httpStatus, endpoint, method, protocolVersion, responseContentType, requestId, responseId, body,
                durationMs, null);
    }

    McpHttpJsonRpcResponse {
        method = method == null || method.isBlank() ? null : method;
        protocolVersion = protocolVersion == null || protocolVersion.isBlank() ? null : protocolVersion;
        responseContentType = responseContentType == null || responseContentType.isBlank()
                ? null
                : responseContentType;
        body = McpMaps.copy(body);
    }

    boolean httpOk() {
        return httpStatus >= 200 && httpStatus < 300;
    }

    Object result() {
        return McpJsonRpcProtocol.result(body);
    }

    Object error() {
        return McpJsonRpcProtocol.error(body);
    }

    McpJsonRpcError jsonRpcError() {
        return McpJsonRpcError.from(error());
    }

    Map<String, Object> metadata() {
        return metadata(failureType);
    }

    Map<String, Object> metadata(McpFailureType overrideFailureType) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(McpHttpMetadata.HTTP_STATUS, httpStatus);
        metadata.put(McpHttpMetadata.ENDPOINT, endpoint);
        if (method != null) {
            metadata.put(McpHttpMetadata.METHOD, method);
        }
        if (protocolVersion != null) {
            metadata.put(McpHttpMetadata.PROTOCOL_VERSION, protocolVersion);
        }
        if (responseContentType != null) {
            metadata.put(McpHttpMetadata.RESPONSE_CONTENT_TYPE, responseContentType);
        }
        putIfPresent(metadata, McpHttpMetadata.REQUEST_ID, requestId);
        putIfPresent(metadata, McpHttpMetadata.RESPONSE_ID, responseId);
        McpFailureType effectiveFailureType = overrideFailureType == null ? failureType : overrideFailureType;
        McpFailureType.putMetadataValue(metadata, effectiveFailureType);
        McpJsonRpcError error = jsonRpcError();
        if (error != null) {
            metadata.put(McpHttpMetadata.JSON_RPC_ERROR, error.metadata());
        }
        return McpMaps.copy(metadata);
    }

    McpFailureType failureTypeOr(McpFailureType fallback) {
        return failureType == null ? fallback : failureType;
    }

    private static void putIfPresent(Map<String, Object> metadata, String key, Object value) {
        if (value instanceof String string && string.isBlank()) {
            return;
        }
        if (value != null) {
            metadata.put(key, value);
        }
    }
}

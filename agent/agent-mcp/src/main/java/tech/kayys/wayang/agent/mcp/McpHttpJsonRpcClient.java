package tech.kayys.wayang.agent.mcp;

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
        return send(context, jsonRpcRequest(method, params));
    }

    private Uni<McpHttpJsonRpcResponse> send(Map<String, Object> context, Map<String, Object> payload) {
        Instant startedAt = Instant.now();
        Optional<String> endpoint = endpoint(context);
        if (endpoint.isEmpty()) {
            return Uni.createFrom().failure(new IllegalArgumentException(
                    "MCP HTTP endpoint is required in invocation context"));
        }

        HttpRequest request;
        try {
            request = buildRequest(context, endpoint.get(), payload);
        } catch (RuntimeException | JsonProcessingException error) {
            return Uni.createFrom().failure(error);
        }

        return Uni.createFrom().completionStage(httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .map(response -> toResponse(response, startedAt));
    }

    private HttpRequest buildRequest(
            Map<String, Object> context,
            String endpoint,
            Map<String, Object> payload) throws JsonProcessingException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(timeout(context))
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json")
                .header(MCP_PROTOCOL_VERSION_HEADER, protocolVersion(context))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)));

        headers(context).forEach(builder::header);
        return builder.build();
    }

    private McpHttpJsonRpcResponse toResponse(HttpResponse<String> response, Instant startedAt) {
        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        try {
            return new McpHttpJsonRpcResponse(
                    response.statusCode(),
                    response.uri().toString(),
                    parseBody(response.body()),
                    durationMs);
        } catch (IOException error) {
            return new McpHttpJsonRpcResponse(
                    response.statusCode(),
                    response.uri().toString(),
                    Map.of("error", Map.of("message", "Failed to parse MCP response: " + error.getMessage())),
                    durationMs);
        }
    }

    private Map<String, Object> jsonRpcRequest(String method, Map<String, Object> params) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jsonrpc", "2.0");
        payload.put("id", UUID.randomUUID().toString());
        payload.put("method", method);
        if (params != null && !params.isEmpty()) {
            payload.put("params", McpMaps.copy(params));
        }
        return payload;
    }

    private Map<String, Object> parseBody(String body) throws IOException {
        String json = extractEventData(body == null ? "" : body);
        if (json.isBlank()) {
            return Map.of();
        }
        return mapper.readValue(json, MAP_TYPE);
    }

    private String extractEventData(String body) {
        if (!body.lines().anyMatch(line -> line.startsWith("data:"))) {
            return body;
        }
        return body.lines()
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring("data:".length()).trim())
                .filter(line -> !line.isBlank())
                .filter(line -> !"[DONE]".equals(line))
                .reduce((previous, current) -> current)
                .orElse("");
    }

    private Optional<String> endpoint(Map<String, Object> context) {
        return stringValue(context, McpTransportContext.KEY_MCP_ENDPOINT)
                .or(() -> stringValue(context, McpTransportContext.KEY_URL));
    }

    private String protocolVersion(Map<String, Object> context) {
        return stringValue(context, McpTransportContext.KEY_PROTOCOL_VERSION)
                .orElse(McpTransportContext.DEFAULT_PROTOCOL_VERSION);
    }

    private Map<String, String> headers(Map<String, Object> context) {
        Map<String, Object> rawHeaders = firstMap(context, McpTransportContext.KEY_MCP_HEADERS)
                .or(() -> firstMap(context, McpTransportContext.KEY_HEADERS))
                .orElse(Map.of());
        Map<String, String> headers = new LinkedHashMap<>();
        rawHeaders.forEach((key, value) -> {
            if (value != null) {
                headers.put(key, String.valueOf(value));
            }
        });
        return Map.copyOf(headers);
    }

    private Duration timeout(Map<String, Object> context) {
        Object value = context == null ? null : context.get(McpTransportContext.KEY_TIMEOUT_MS);
        if (value instanceof Number number && number.longValue() > 0) {
            return Duration.ofMillis(number.longValue());
        }
        if (value instanceof String text) {
            try {
                long millis = Long.parseLong(text);
                if (millis > 0) {
                    return Duration.ofMillis(millis);
                }
            } catch (NumberFormatException ignored) {
                return Duration.ofSeconds(30);
            }
        }
        return Duration.ofSeconds(30);
    }

    private Optional<Map<String, Object>> firstMap(Map<String, Object> values, String key) {
        return Optional.of(McpMaps.fromObject(values == null ? null : values.get(key)))
                .filter(map -> !map.isEmpty());
    }

    private Optional<String> stringValue(Map<String, Object> values, String key) {
        Object value = values == null ? null : values.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return Optional.of(text.trim());
        }
        return Optional.empty();
    }
}

record McpHttpJsonRpcResponse(
        int httpStatus,
        String endpoint,
        Map<String, Object> body,
        long durationMs) {

    McpHttpJsonRpcResponse {
        body = McpMaps.copy(body);
    }

    boolean httpOk() {
        return httpStatus >= 200 && httpStatus < 300;
    }

    Object result() {
        return body.containsKey("result") ? body.get("result") : body;
    }

    Object error() {
        return body.get("error");
    }

    Map<String, Object> metadata() {
        return Map.of(
                "endpoint", endpoint,
                "httpStatus", httpStatus);
    }
}

package tech.kayys.wayang.tool.rest;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.wayang.tool.executor.AbstractToolExecutor;
import tech.kayys.wayang.tool.node.ToolNodeTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Executor for RESTful API tools.
 * Provides higher-level abstractions over generic HTTP calls.
 */
@ApplicationScoped
public class RESTToolExecutor extends AbstractToolExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(RESTToolExecutor.class);

    @Inject
    Vertx vertx;

    private WebClient webClient;

    @PostConstruct
    void init() {
        this.webClient = WebClient.create(vertx, new WebClientOptions()
                .setUserAgent("Wayang-REST-Executor/1.0.0")
                .setConnectTimeout(30000));
    }

    @Override
    public String getExecutorType() {
        return ToolNodeTypes.TOOL_REST;
    }

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Map<String, Object> context = task.context();
        String baseUrl = (String) context.get("baseUrl");
        String path = (String) context.get("path");
        String method = (String) context.getOrDefault("method", "GET");
        Map<String, Object> headers = (Map<String, Object>) context.getOrDefault("headers", Map.of());
        Map<String, Object> queryParams = (Map<String, Object>) context.getOrDefault("queryParams", Map.of());
        Object body = context.get("body");

        Instant startedAt = Instant.now();

        if (baseUrl == null || baseUrl.isBlank()) {
            return Uni.createFrom().item(failure(task, "Base URL is required for REST tool execution", startedAt));
        }

        String fullUrl = baseUrl;
        if (path != null && !path.isBlank()) {
            if (!fullUrl.endsWith("/") && !path.startsWith("/")) {
                fullUrl += "/";
            } else if (fullUrl.endsWith("/") && path.startsWith("/")) {
                fullUrl = fullUrl.substring(0, fullUrl.length() - 1);
            }
            fullUrl += path;
        }

        LOG.info("Executing REST {} request to {}", method, fullUrl);

        var request = switch (method.toUpperCase()) {
            case "POST" -> webClient.postAbs(fullUrl);
            case "PUT" -> webClient.putAbs(fullUrl);
            case "DELETE" -> webClient.deleteAbs(fullUrl);
            case "PATCH" -> webClient.patchAbs(fullUrl);
            default -> webClient.getAbs(fullUrl);
        };

        // Enforce JSON by default
        request.putHeader("Accept", "application/json");
        if (body != null) {
            request.putHeader("Content-Type", "application/json");
        }

        // Add custom headers
        headers.forEach((k, v) -> request.putHeader(k, String.valueOf(v)));

        // Add query parameters
        queryParams.forEach((k, v) -> request.addQueryParam(k, String.valueOf(v)));

        Uni<io.vertx.mutiny.ext.web.client.HttpResponse<io.vertx.mutiny.core.buffer.Buffer>> responseUni;
        if (body != null && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method))) {
            if (body instanceof Map) {
                responseUni = request.sendJsonObject(JsonObject.mapFrom(body));
            } else if (body instanceof String) {
                responseUni = request.sendBuffer(io.vertx.mutiny.core.buffer.Buffer.buffer((String) body));
            } else {
                responseUni = request.sendJsonObject(JsonObject.mapFrom(body));
            }
        } else {
            responseUni = request.send();
        }

        return responseUni
                .map(resp -> {
                    Map<String, Object> output = Map.of(
                            "status", resp.statusCode(),
                            "headers", resp.headers().getDelegate().entries(),
                            "body", resp.bodyAsString());
                    return success(task, output, startedAt);
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("REST request failed: {}", throwable.getMessage());
                    return failure(task, throwable.getMessage(), startedAt);
                });
    }
}

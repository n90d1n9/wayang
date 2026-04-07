package tech.kayys.wayang.tool.http;

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
 * Executor for HTTP tools.
 */
@ApplicationScoped
public class HTTPToolExecutor extends AbstractToolExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(HTTPToolExecutor.class);

    @Inject
    Vertx vertx;

    private WebClient webClient;

    @PostConstruct
    void init() {
        this.webClient = WebClient.create(vertx, new WebClientOptions()
                .setUserAgent("Wayang-HTTP-Executor/1.0.0")
                .setConnectTimeout(30000));
    }

    @Override
    public String getExecutorType() {
        return ToolNodeTypes.TOOL_HTTP;
    }

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Map<String, Object> context = task.context();
        String url = (String) context.get("url");
        String method = (String) context.getOrDefault("method", "GET");
        Map<String, Object> headers = (Map<String, Object>) context.getOrDefault("headers", Map.of());
        Object body = context.get("body");

        Instant startedAt = Instant.now();
        if (url == null || url.isBlank()) {
            return Uni.createFrom().item(failure(task, "URL is required for HTTP tool execution", startedAt));
        }

        LOG.info("Executing HTTP {} request to {}", method, url);

        var request = switch (method.toUpperCase()) {
            case "POST" -> webClient.postAbs(url);
            case "PUT" -> webClient.putAbs(url);
            case "DELETE" -> webClient.deleteAbs(url);
            case "PATCH" -> webClient.patchAbs(url);
            default -> webClient.getAbs(url);
        };

        // Add headers
        headers.forEach((k, v) -> request.putHeader(k, String.valueOf(v)));

        Uni<io.vertx.mutiny.ext.web.client.HttpResponse<io.vertx.mutiny.core.buffer.Buffer>> responseUni;
        if (body != null && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method))) {
            if (body instanceof Map) {
                responseUni = request.sendJsonObject(JsonObject.mapFrom(body));
            } else {
                responseUni = request.sendBuffer(io.vertx.mutiny.core.buffer.Buffer.buffer(String.valueOf(body)));
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
                    LOG.error("HTTP request failed: {}", throwable.getMessage());
                    return failure(task, throwable.getMessage(), startedAt);
                });
    }
}

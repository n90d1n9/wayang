package tech.kayys.wayang.agent.skills.builtin;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Makes outbound HTTP GET/POST calls to external APIs or internal services.
 *
 * <p>
 * Useful for connecting an agent to REST APIs, webhooks, and microservices
 * without writing custom Java code. The response body (text or JSON) is
 * returned
 * as the skill observation and injected into the agent's reasoning context.
 * </p>
 *
 * <h2>Inputs</h2>
 * <table border="1">
 * <tr>
 * <th>Key</th>
 * <th>Required</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>url</td>
 * <td>yes</td>
 * <td>Full URL to call</td>
 * </tr>
 * <tr>
 * <td>method</td>
 * <td>no</td>
 * <td>GET (default) | POST | PUT | DELETE</td>
 * </tr>
 * <tr>
 * <td>body</td>
 * <td>no</td>
 * <td>Request body for POST/PUT (JSON string)</td>
 * </tr>
 * <tr>
 * <td>content_type</td>
 * <td>no</td>
 * <td>Content-Type header (default application/json)</td>
 * </tr>
 * <tr>
 * <td>headers</td>
 * <td>no</td>
 * <td>Map&lt;String,String&gt; of extra headers</td>
 * </tr>
 * <tr>
 * <td>timeout_seconds</td>
 * <td>no</td>
 * <td>Request timeout (default 15)</td>
 * </tr>
 * </table>
 *
 * <h2>Outputs</h2>
 * <ul>
 * <li>{@code status_code} – HTTP status code</li>
 * <li>{@code response_body} – response text (truncated to 4 KB)</li>
 * <li>{@code content_type} – response Content-Type header</li>
 * </ul>
 *
 * <h2>Security</h2>
 * Operators should configure an allowlist of hosts via
 * {@code gollek.agent.skills.http.allowed-hosts} (comma-separated).
 * When set, requests to non-listed hosts are rejected before any I/O.
 */
@ApplicationScoped
@SkillDescriptor(id = "http_call", name = "HTTP Call", description = "Makes HTTP GET or POST requests to external REST APIs and returns the response.", version = "1.0.0", category = SkillCategory.COMMUNICATION, inputs = {
        @SkillDescriptor.Input(name = "url", description = "Full URL to call"),
        @SkillDescriptor.Input(name = "method", required = false, description = "GET (default) | POST | PUT | DELETE"),
        @SkillDescriptor.Input(name = "body", required = false, description = "Request body for POST/PUT (JSON string)"),
        @SkillDescriptor.Input(name = "content_type", required = false, description = "Content-Type header (default application/json)"),
        @SkillDescriptor.Input(name = "headers", type = "object", required = false, description = "Map of extra headers"),
        @SkillDescriptor.Input(name = "timeout_seconds", type = "integer", required = false, description = "Request timeout (default 15)")
}, outputs = {
        @SkillDescriptor.Output(name = "status_code", type = "integer", description = "HTTP status code"),
        @SkillDescriptor.Output(name = "response_body", description = "The raw response content"),
        @SkillDescriptor.Output(name = "content_type", description = "Response Content-Type")
}, triggers = { "api", "http", "fetch", "request", "rest", "endpoint" }, priority = 50)
public class HttpCallSkill implements AgentSkill {

    private static final Logger LOG = Logger.getLogger(HttpCallSkill.class);
    private static final int MAX_RESPONSE_CHARS = 4096;

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "gollek.agent.skills.http-call.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "gollek.agent.skills.http-call.default-timeout-seconds", defaultValue = "15")
    int defaultTimeout;

    @ConfigProperty(name = "gollek.agent.skills.http-call.allowed-hosts", defaultValue = "")
    String allowedHosts;

    private WebClient webClient;
    private java.util.Set<String> allowedHostSet;

    @PostConstruct
    void init() {
        webClient = WebClient.create(vertx);
        allowedHostSet = allowedHosts.isBlank()
                ? java.util.Set.of()
                : java.util.Arrays.stream(allowedHosts.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public String id() {
        return "http_call";
    }

    @Override
    public String name() {
        return "HTTP Call";
    }

    @Override
    public String description() {
        return "Makes HTTP requests to REST APIs.";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public SkillCategory category() {
        return SkillCategory.COMMUNICATION;
    }

    @Override
    public boolean canHandle(Map<String, Object> inputs) {
        return enabled && inputs.containsKey("url");
    }

    @Override
    public Uni<SkillResult> execute(SkillContext ctx) {
        Instant start = Instant.now();
        String url = ctx.requireInput("url", String.class);
        String method = ctx.getStringInput("method", "GET").toUpperCase();
        String body = ctx.getStringInput("body", null);
        String contentType = ctx.getStringInput("content_type", "application/json");
        int timeout = ctx.getIntInput("timeout_seconds", defaultTimeout);

        // Host allowlist check
        if (!allowedHostSet.isEmpty()) {
            try {
                java.net.URI uri = java.net.URI.create(url);
                if (!allowedHostSet.contains(uri.getHost())) {
                    return Uni.createFrom().item(SkillResult.builder()
                            .skillId(id())
                            .invocationId(ctx.invocationId())
                            .status(SkillResult.Status.FAILURE)
                            .observation("Host not in allowlist: " + uri.getHost())
                            .durationMs(Duration.between(start, Instant.now()).toMillis())
                            .build());
                }
            } catch (Exception e) {
                return Uni.createFrom().item(SkillResult.builder()
                        .skillId(id())
                        .invocationId(ctx.invocationId())
                        .status(SkillResult.Status.FAILURE)
                        .observation("Invalid URL: " + url)
                        .durationMs(Duration.between(start, Instant.now()).toMillis())
                        .build());
            }
        }

        Uni<HttpResponse<Buffer>> request;
        switch (method) {
            case "POST" -> request = webClient.postAbs(url)
                    .timeout(timeout * 1000L)
                    .putHeader("Content-Type", contentType)
                    .sendBuffer(Buffer.buffer(body != null ? body : ""));
            case "PUT" -> request = webClient.putAbs(url)
                    .timeout(timeout * 1000L)
                    .putHeader("Content-Type", contentType)
                    .sendBuffer(Buffer.buffer(body != null ? body : ""));
            case "DELETE" -> request = webClient.deleteAbs(url)
                    .timeout(timeout * 1000L).send();
            default -> request = webClient.getAbs(url).timeout(timeout * 1000L).send();
        }

        return request.map(resp -> {
            int status = resp.statusCode();
            String responseBody = resp.bodyAsString();
            if (responseBody != null && responseBody.length() > MAX_RESPONSE_CHARS) {
                responseBody = responseBody.substring(0, MAX_RESPONSE_CHARS) + "…[truncated]";
            }
            long durationMs = Duration.between(start, Instant.now()).toMillis();

            if (status >= 200 && status < 300) {
                return SkillResult.builder()
                        .skillId(id())
                        .invocationId(ctx.invocationId())
                        .status(SkillResult.Status.SUCCESS)
                        .observation(responseBody)
                        .output("status_code", status)
                        .output("response_body", responseBody != null ? responseBody : "")
                        .durationMs(durationMs)
                        .build();
            } else {
                return SkillResult.builder()
                        .skillId(id())
                        .invocationId(ctx.invocationId())
                        .status(SkillResult.Status.FAILURE)
                        .observation("HTTP " + status + ": " + responseBody)
                        .durationMs(durationMs)
                        .build();
            }
        }).onFailure().recoverWithItem(err -> {
            LOG.errorf(err, "HTTP call failed: %s", url);
            return SkillResult.builder()
                    .skillId(id())
                    .invocationId(ctx.invocationId())
                    .status(SkillResult.Status.ERROR)
                    .observation(err.getMessage())
                    .error(err)
                    .durationMs(Duration.between(start, Instant.now()).toMillis())
                    .build();
        });
    }
}

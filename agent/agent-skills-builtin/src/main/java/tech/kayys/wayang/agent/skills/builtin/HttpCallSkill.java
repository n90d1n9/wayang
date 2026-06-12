package tech.kayys.wayang.agent.skills.builtin;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDescriptor;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@SkillDescriptor(id = "http_call", name = "HTTP Call", description = "Makes HTTP GET, POST, PUT, or DELETE requests and returns the response.", version = "1.0.0", category = SkillCategory.COMMUNICATION, inputs = {
        @SkillDescriptor.Input(name = "url", description = "Full URL to call"),
        @SkillDescriptor.Input(name = "method", required = false, description = "GET (default) | POST | PUT | DELETE"),
        @SkillDescriptor.Input(name = "body", required = false, description = "Request body for POST/PUT"),
        @SkillDescriptor.Input(name = "content_type", required = false, description = "Content-Type header"),
        @SkillDescriptor.Input(name = "headers", type = "object", required = false, description = "Map of extra headers")
}, outputs = {
        @SkillDescriptor.Output(name = "status_code", type = "integer", description = "HTTP status code"),
        @SkillDescriptor.Output(name = "response_body", description = "The raw response content")
}, triggers = { "api", "http", "fetch", "request", "rest", "endpoint" }, priority = 50)
public class HttpCallSkill implements AgentSkill {

    private static final Logger LOG = Logger.getLogger(HttpCallSkill.class);
    private static final int MAX_RESPONSE_CHARS = 4096;

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "gollek.agent.skills.http-call.enabled", defaultValue = "true")
    boolean enabled = true;

    @ConfigProperty(name = "gollek.agent.skills.http-call.default-timeout-seconds", defaultValue = "15")
    int defaultTimeout = 15;

    @ConfigProperty(name = "gollek.agent.skills.http-call.allowed-hosts", defaultValue = "")
    String allowedHosts = "";

    private WebClient webClient;
    private Set<String> allowedHostSet = Set.of();

    @PostConstruct
    void init() {
        if (vertx != null) {
            webClient = WebClient.create(vertx);
        }
        allowedHostSet = allowedHosts == null || allowedHosts.isBlank()
                ? Set.of()
                : java.util.Arrays.stream(allowedHosts.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
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
    public String category() {
        return SkillCategory.COMMUNICATION.name();
    }

    @Override
    public boolean canHandle(Map<String, Object> inputs) {
        return enabled && inputs != null && inputs.containsKey("url");
    }

    @Override
    public Uni<Map<String, Object>> execute(Map<String, Object> context) {
        Instant start = Instant.now();
        Map<String, Object> inputs = context == null ? Map.of() : context;
        String url = BuiltinSkillSupport.stringInput(inputs, "url");
        if (url == null || url.isBlank()) {
            return Uni.createFrom().item(BuiltinSkillSupport.failure("Input 'url' is required"));
        }
        if (webClient == null) {
            return Uni.createFrom().item(BuiltinSkillSupport.failure("HTTP client is not initialized"));
        }
        String method = BuiltinSkillSupport.stringInput(inputs, "method", "GET").toUpperCase(java.util.Locale.ROOT);
        String body = BuiltinSkillSupport.stringInput(inputs, "body", null);
        String contentType = BuiltinSkillSupport.stringInput(inputs, "content_type", "application/json");
        int timeout = BuiltinSkillSupport.intInput(inputs, "timeout_seconds", defaultTimeout);
        Map<String, String> headers = BuiltinSkillSupport.stringMapInput(inputs, "headers");

        String blockedHost = blockedHost(url);
        if (blockedHost != null) {
            return Uni.createFrom().item(BuiltinSkillSupport.failure("Host not in allowlist: " + blockedHost));
        }

        Uni<HttpResponse<Buffer>> request = switch (method) {
            case "POST" -> applyHeaders(webClient.postAbs(url)
                    .timeout(timeout * 1000L)
                    .putHeader("Content-Type", contentType), headers)
                    .sendBuffer(Buffer.buffer(body != null ? body : ""));
            case "PUT" -> applyHeaders(webClient.putAbs(url)
                    .timeout(timeout * 1000L)
                    .putHeader("Content-Type", contentType), headers)
                    .sendBuffer(Buffer.buffer(body != null ? body : ""));
            case "DELETE" -> applyHeaders(webClient.deleteAbs(url)
                    .timeout(timeout * 1000L), headers)
                    .send();
            default -> applyHeaders(webClient.getAbs(url)
                    .timeout(timeout * 1000L), headers)
                    .send();
        };

        return request.map(response -> toResult(response, start))
                .onFailure().recoverWithItem(error -> {
                    LOG.errorf(error, "HTTP call failed: %s", url);
                    return BuiltinSkillSupport.error(error);
                });
    }

    private String blockedHost(String url) {
        if (allowedHostSet.isEmpty()) {
            return null;
        }
        try {
            String host = java.net.URI.create(url).getHost();
            return allowedHostSet.contains(host) ? null : host;
        } catch (Exception e) {
            return url;
        }
    }

    private HttpRequest<Buffer> applyHeaders(HttpRequest<Buffer> request, Map<String, String> headers) {
        headers.forEach(request::putHeader);
        return request;
    }

    private Map<String, Object> toResult(HttpResponse<Buffer> response, Instant start) {
        int status = response.statusCode();
        String body = response.bodyAsString();
        if (body != null && body.length() > MAX_RESPONSE_CHARS) {
            body = body.substring(0, MAX_RESPONSE_CHARS) + "...[truncated]";
        }
        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("status_code", status);
        outputs.put("response_body", body == null ? "" : body);
        outputs.put("content_type", response.getHeader("Content-Type"));
        outputs.put("durationMs", Duration.between(start, Instant.now()).toMillis());
        if (status >= 200 && status < 300) {
            return BuiltinSkillSupport.success(body, outputs);
        }
        Map<String, Object> failure = BuiltinSkillSupport.failure("HTTP " + status + ": " + body);
        failure.putAll(outputs);
        return failure;
    }
}

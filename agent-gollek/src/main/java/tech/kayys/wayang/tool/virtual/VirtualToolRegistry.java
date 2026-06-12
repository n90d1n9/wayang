package tech.kayys.gamelan.tool.virtual;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.tool.ToolHandler;
import tech.kayys.gamelan.tool.ToolResult;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Tool Virtualization Layer — a unified interface for heterogeneous tool backends.
 *
 * <h2>Problem</h2>
 * Tools are currently hardcoded to local filesystem and shell. Real production
 * agentic systems need to invoke:
 * <ul>
 *   <li>REST APIs (internal microservices, third-party SaaS)</li>
 *   <li>Databases (JDBC, Mongo, Redis)</li>
 *   <li>Local functions (Java lambdas registered at runtime)</li>
 *   <li>External agents (sub-agents over HTTP or MCP)</li>
 *   <li>Message queues (publish events, consume results)</li>
 * </ul>
 * Without a virtualization layer, each new tool type requires custom CDI beans
 * and a recompile. With it, tools are registered at runtime from configuration.
 *
 * <h2>Capability Discovery Protocol</h2>
 * Agents can dynamically discover what tools exist:
 * <pre>
 * <tool_call>
 *   <n>discover_capabilities</n>
 *   <filter>database</filter>
 * </tool_call>
 * </pre>
 * Returns a JSON catalogue of all registered virtual tools matching the filter.
 * Agents then call any discovered tool by name without hardcoded knowledge.
 *
 * <h2>Tool Registration</h2>
 * <pre>
 * // REST API backend
 * registry.register(VirtualTool.rest("customer-api",
 *     "Query customer data", "https://api.example.com/customers/{id}",
 *     Map.of("Authorization", "Bearer ${API_TOKEN}")));
 *
 * // Java lambda backend
 * registry.register(VirtualTool.lambda("format-currency",
 *     "Format a number as currency", params ->
 *         ToolResult.success("format-currency",
 *             "$" + String.format("%.2f", Double.parseDouble(params.get("amount"))))));
 *
 * // External agent backend
 * registry.register(VirtualTool.agent("security-scanner",
 *     "Run security analysis", "http://security-agent:8080/analyze"));
 * </pre>
 *
 * <h2>Timeout and Retry Policy</h2>
 * Every virtual tool has a configurable timeout and retry count. Failed calls
 * use exponential backoff. Circuit breaker opens after 3 consecutive failures.
 */
@ApplicationScoped
public class VirtualToolRegistry implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(VirtualToolRegistry.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int MAX_RETRIES = 2;

    private final Map<String, VirtualTool>       tools          = new ConcurrentHashMap<>();
    private final Map<String, CircuitBreaker>    breakers       = new ConcurrentHashMap<>();
    private final HttpClient                     http;

    public VirtualToolRegistry() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    @PostConstruct
    void init() {
        // Register built-in capability discovery tool
        register(VirtualTool.lambda(
                "discover_capabilities",
                "Discover available tools and APIs. Returns a catalogue of registered " +
                "virtual tools matching an optional filter.",
                params -> {
                    String filter = params.getOrDefault("filter", "").toLowerCase();
                    StringBuilder sb = new StringBuilder();
                    sb.append("Available virtual tools:\n\n");
                    tools.values().stream()
                            .filter(t -> filter.isBlank()
                                    || t.name().contains(filter)
                                    || t.description().toLowerCase().contains(filter))
                            .forEach(t -> sb.append(String.format(
                                    "- **%s** [%s]: %s\n", t.name(), t.backend(), t.description())));
                    return ToolResult.success("discover_capabilities", sb.toString());
                }));

        // Register built-in HTTP fetch tool (generic REST caller)
        register(VirtualTool.lambda(
                "http_fetch",
                "Make an HTTP request to any URL. Parameters: url, method (GET/POST), " +
                "headers (JSON), body (JSON or string).",
                params -> executeHttpFetch(params)));

        log.info("[vtool] VirtualToolRegistry initialized with {} built-in tools", tools.size());
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Registers a virtual tool. The tool becomes immediately available to agents.
     */
    public void register(VirtualTool tool) {
        tools.put(tool.name(), tool);
        breakers.put(tool.name(), new CircuitBreaker(tool.name(), 3, Duration.ofMinutes(5)));
        log.info("[vtool] registered: {} [{}]", tool.name(), tool.backend());
    }

    /**
     * Unregisters a tool by name.
     */
    public void unregister(String name) {
        tools.remove(name);
        breakers.remove(name);
    }

    /** Returns all registered virtual tools (including built-ins). */
    public Collection<VirtualTool> all() { return Collections.unmodifiableCollection(tools.values()); }

    /** Returns tool by name. */
    public Optional<VirtualTool> find(String name) { return Optional.ofNullable(tools.get(name)); }

    // ── ToolHandler implementation ─────────────────────────────────────────

    @Override
    public String toolName() { return "virtual"; }

    @Override
    public List<String> toolNames() {
        // Expose every registered virtual tool as a native tool name
        List<String> names = new ArrayList<>(tools.keySet());
        names.add("virtual");
        return names;
    }

    @Override
    public String description() {
        return "Virtual tool registry — dynamically dispatches to REST APIs, " +
               "lambdas, and external agents. Use 'discover_capabilities' to list available tools.";
    }

    @Override
    public List<String> parameters() {
        return List.of(
                "Any parameters accepted by the specific virtual tool being called",
                "Use discover_capabilities to learn what parameters each tool accepts"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String name = call.name();

        // Handle capability discovery specially
        if ("virtual".equals(name)) {
            return tools.get("discover_capabilities").execute(call.parameters());
        }

        VirtualTool tool = tools.get(name);
        if (tool == null) {
            return ToolResult.failure(name, "Virtual tool '" + name + "' not registered. " +
                    "Use discover_capabilities to list available tools.");
        }

        CircuitBreaker breaker = breakers.get(name);
        if (breaker != null && breaker.isOpen()) {
            return ToolResult.failure(name,
                    "Circuit breaker OPEN for '" + name + "' — too many recent failures. " +
                    "Will retry after " + breaker.retryAfterSeconds() + "s.");
        }

        // Execute with retry + circuit breaker
        return executeWithRetry(tool, call, breaker);
    }

    // ── Execution ──────────────────────────────────────────────────────────

    private ToolResult executeWithRetry(VirtualTool tool, ToolCall call, CircuitBreaker breaker) {
        int timeout = tool.timeoutSeconds() > 0 ? tool.timeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                long backoffMs = (long) Math.pow(2, attempt) * 200;
                try { Thread.sleep(backoffMs); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return ToolResult.failure(tool.name(), "Interrupted during retry");
                }
                log.info("[vtool] retry {}/{} for '{}'", attempt, MAX_RETRIES, tool.name());
            }

            try {
                ToolResult result = dispatchToBackend(tool, call, timeout);
                if (result.isSuccess()) {
                    if (breaker != null) breaker.recordSuccess();
                    return result;
                }
                if (attempt == MAX_RETRIES) {
                    if (breaker != null) breaker.recordFailure();
                    return result;
                }
            } catch (Exception e) {
                if (attempt == MAX_RETRIES) {
                    if (breaker != null) breaker.recordFailure();
                    return ToolResult.failure(tool.name(), "Execution failed: " + e.getMessage());
                }
            }
        }
        return ToolResult.failure(tool.name(), "All retries exhausted");
    }

    private ToolResult dispatchToBackend(VirtualTool tool, ToolCall call, int timeoutSeconds)
            throws Exception {
        return switch (tool.backend()) {
            case REST   -> executeRest(tool, call, timeoutSeconds);
            case LAMBDA -> tool.execute(call.parameters());
            case AGENT  -> executeAgent(tool, call, timeoutSeconds);
            case MOCK   -> ToolResult.success(tool.name(), "MOCK: " + call.parameters());
        };
    }

    private ToolResult executeRest(VirtualTool tool, ToolCall call, int timeoutSeconds)
            throws Exception {
        String url = tool.endpoint();
        // Interpolate path params: /customers/{id} → /customers/123
        for (Map.Entry<String, String> e : call.parameters().entrySet()) {
            url = url.replace("{" + e.getKey() + "}", e.getValue());
        }

        String method = call.param("method", "GET").toUpperCase();
        String body   = call.param("body", "");

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds));

        // Apply tool-level headers (resolved from env vars)
        tool.headers().forEach((k, v) -> reqBuilder.header(k, resolveEnvVars(v)));

        HttpRequest req = switch (method) {
            case "POST", "PUT" -> reqBuilder.method(method,
                    HttpRequest.BodyPublishers.ofString(body)).build();
            case "DELETE" -> reqBuilder.DELETE().build();
            default -> reqBuilder.GET().build();
        };

        HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return ToolResult.success(tool.name(), response.body());
        } else {
            return ToolResult.failure(tool.name(),
                    "HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    private ToolResult executeAgent(VirtualTool tool, ToolCall call, int timeoutSeconds)
            throws Exception {
        // Forward the tool call to an external agent over HTTP
        String payload = MAPPER.writeValueAsString(Map.of(
                "tool", call.name(),
                "params", call.parameters()));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(tool.endpoint()))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return ToolResult.success(tool.name(), response.body());
        } else {
            return ToolResult.failure(tool.name(), "Agent error: " + response.body());
        }
    }

    private ToolResult executeHttpFetch(Map<String, String> params) {
        try {
            String url    = params.getOrDefault("url", "");
            if (url.isBlank()) return ToolResult.failure("http_fetch", "'url' is required");

            String method = params.getOrDefault("method", "GET").toUpperCase();
            String body   = params.getOrDefault("body", "");

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));

            // Parse additional headers from JSON
            String headersJson = params.getOrDefault("headers", "{}");
            try {
                Map<?, ?> headers = MAPPER.readValue(headersJson, Map.class);
                headers.forEach((k, v) -> builder.header(k.toString(), v.toString()));
            } catch (Exception ignored) {}

            HttpRequest req = switch (method) {
                case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
                case "PUT"  -> builder.PUT(HttpRequest.BodyPublishers.ofString(body)).build();
                case "DELETE" -> builder.DELETE().build();
                default -> builder.GET().build();
            };

            HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
            String result = String.format("HTTP %d\n\n%s", response.statusCode(), response.body());
            return response.statusCode() < 400
                    ? ToolResult.success("http_fetch", result)
                    : ToolResult.failure("http_fetch", result);
        } catch (Exception e) {
            return ToolResult.failure("http_fetch", "Request failed: " + e.getMessage());
        }
    }

    private String resolveEnvVars(String value) {
        if (!value.contains("${")) return value;
        StringBuilder result = new StringBuilder(value);
        int start;
        while ((start = result.indexOf("${")) >= 0) {
            int end = result.indexOf("}", start);
            if (end < 0) break;
            String varName = result.substring(start + 2, end);
            String resolved = System.getenv(varName);
            if (resolved == null) resolved = System.getProperty(varName, "");
            result.replace(start, end + 1, resolved);
        }
        return result.toString();
    }

    // ── Circuit breaker ────────────────────────────────────────────────────

    static class CircuitBreaker {
        private final String   name;
        private final int      failureThreshold;
        private final Duration resetTimeout;
        private int            failures    = 0;
        private boolean        open        = false;
        private long           openedAt    = 0;

        CircuitBreaker(String name, int threshold, Duration timeout) {
            this.name = name; this.failureThreshold = threshold; this.resetTimeout = timeout;
        }

        synchronized void recordFailure() {
            failures++;
            if (failures >= failureThreshold && !open) {
                open     = true;
                openedAt = System.currentTimeMillis();
                log.warn("[circuit-breaker] OPENED for '{}' after {} failures", name, failures);
            }
        }

        synchronized void recordSuccess() {
            failures = 0;
            open     = false;
        }

        synchronized boolean isOpen() {
            if (open) {
                long elapsed = System.currentTimeMillis() - openedAt;
                if (elapsed > resetTimeout.toMillis()) {
                    open     = false;
                    failures = 0;
                    log.info("[circuit-breaker] HALF-OPEN for '{}'", name);
                    return false;
                }
            }
            return open;
        }

        long retryAfterSeconds() {
            return (resetTimeout.toMillis() - (System.currentTimeMillis() - openedAt)) / 1000;
        }
    }

    // ── Data types ─────────────────────────────────────────────────────────

    /**
     * A registered virtual tool with its backend configuration.
     */
    public record VirtualTool(
            String              name,
            String              description,
            Backend             backend,
            String              endpoint,      // URL for REST/AGENT backends
            Map<String, String> headers,       // HTTP headers
            int                 timeoutSeconds,
            Function<Map<String, String>, ToolResult> executor  // for LAMBDA backend
    ) {
        public enum Backend { REST, LAMBDA, AGENT, MOCK }

        static VirtualTool rest(String name, String description, String url,
                                Map<String, String> headers) {
            return new VirtualTool(name, description, Backend.REST, url,
                    headers, 30, null);
        }

        static VirtualTool lambda(String name, String description,
                                  Function<Map<String, String>, ToolResult> fn) {
            return new VirtualTool(name, description, Backend.LAMBDA, null,
                    Map.of(), 10, fn);
        }

        static VirtualTool agent(String name, String description, String agentUrl) {
            return new VirtualTool(name, description, Backend.AGENT, agentUrl,
                    Map.of("Content-Type", "application/json"), 60, null);
        }

        static VirtualTool mock(String name, String description) {
            return new VirtualTool(name, description, Backend.MOCK, null, Map.of(), 1, null);
        }

        ToolResult execute(Map<String, String> params) {
            if (executor != null) return executor.apply(params);
            return ToolResult.failure(name, "No executor for backend: " + backend);
        }
    }
}

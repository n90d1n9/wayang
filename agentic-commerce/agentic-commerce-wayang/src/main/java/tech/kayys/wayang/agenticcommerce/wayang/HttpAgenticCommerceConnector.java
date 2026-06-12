package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceError;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpResponse;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Java HTTP client backed Agentic Commerce seller connector.
 */
public final class HttpAgenticCommerceConnector implements AgenticCommerceConnector {

    public static final String CONNECTOR_NAME = "http";
    public static final long DEFAULT_TIMEOUT_MILLIS = 30_000L;

    private static final Set<String> SKIPPED_REQUEST_HEADERS = Set.of(
            "connection",
            "content-length",
            "expect",
            "host",
            "upgrade");

    private final AgenticCommerceConnectorConfig config;
    private final HttpClient client;
    private final Duration timeout;

    public HttpAgenticCommerceConnector(AgenticCommerceConnectorConfig config) {
        this(config, null);
    }

    public HttpAgenticCommerceConnector(
            AgenticCommerceConnectorConfig config,
            HttpClient client) {
        this.config = config == null ? AgenticCommerceConnectorConfig.defaults() : config;
        if (this.config.baseUrl().isBlank()) {
            throw new IllegalArgumentException("Agentic Commerce HTTP connector baseUrl must not be blank");
        }
        this.timeout = timeout(this.config);
        this.client = client == null
                ? HttpClient.newBuilder()
                        .connectTimeout(timeout)
                        .build()
                : client;
    }

    public AgenticCommerceConnectorConfig config() {
        return config;
    }

    public Duration timeout() {
        return timeout;
    }

    @Override
    public AgenticCommerceHttpResponse exchange(AgenticCommerceHttpRequest request) {
        AgenticCommerceHttpRequest resolved = Objects.requireNonNull(request, "request");
        long startedAt = System.nanoTime();
        try {
            HttpResponse<String> response = client.send(
                    httpRequest(resolved),
                    HttpResponse.BodyHandlers.ofString());
            return new AgenticCommerceHttpResponse(
                    response.statusCode(),
                    response.body(),
                    responseHeaders(response),
                    responseAttributes(resolved, startedAt, ""));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return transportError(resolved, startedAt, exception);
        } catch (IOException | IllegalArgumentException exception) {
            return transportError(resolved, startedAt, exception);
        }
    }

    private HttpRequest httpRequest(AgenticCommerceHttpRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(request))
                .timeout(timeout);
        requestHeaders(request).forEach(builder::header);
        builder.method(
                request.method(),
                request.body().isBlank()
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(request.body()));
        return builder.build();
    }

    private URI uri(AgenticCommerceHttpRequest request) {
        return URI.create(config.baseUrl() + request.path());
    }

    private Map<String, String> requestHeaders(AgenticCommerceHttpRequest request) {
        Map<String, Object> values = new LinkedHashMap<>(request.headers());
        values.putAll(config.headers());
        if (!config.apiVersion().isBlank()) {
            values.put(AgenticCommerceProtocol.HEADER_API_VERSION, config.apiVersion());
        }
        if (!config.bearerToken().isBlank()) {
            values.put(AgenticCommerceProtocol.HEADER_AUTHORIZATION, authorizationHeader(config.bearerToken()));
        }
        values.putIfAbsent(AgenticCommerceProtocol.HEADER_ACCEPT, AgenticCommerceProtocol.MIME_JSON);
        if (!request.body().isBlank()) {
            values.putIfAbsent(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, AgenticCommerceProtocol.MIME_JSON);
        }
        Map<String, String> headers = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String name = AgenticCommerceWayangMaps.text(key);
            String headerValue = AgenticCommerceWayangMaps.text(value);
            if (!name.isBlank()
                    && !headerValue.isBlank()
                    && !SKIPPED_REQUEST_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                headers.put(name, headerValue);
            }
        });
        return Map.copyOf(headers);
    }

    private Map<String, Object> responseHeaders(HttpResponse<?> response) {
        Map<String, Object> values = new LinkedHashMap<>();
        response.headers().map().forEach((key, value) -> values.put(key, joined(value)));
        return Map.copyOf(values);
    }

    private Map<String, Object> responseAttributes(
            AgenticCommerceHttpRequest request,
            long startedAt,
            String transportError) {
        Map<String, Object> values = new LinkedHashMap<>(request.attributes());
        values.put(AgenticCommerceWayang.METADATA_CONNECTOR, CONNECTOR_NAME);
        values.put("connectorBaseUrl", config.baseUrl());
        values.put("transportDurationMillis", durationMillis(startedAt));
        if (!transportError.isBlank()) {
            values.put("transportError", transportError);
        }
        return Map.copyOf(values);
    }

    private AgenticCommerceHttpResponse transportError(
            AgenticCommerceHttpRequest request,
            long startedAt,
            Exception exception) {
        Map<String, Object> body = Map.of(
                "error",
                AgenticCommerceError.of(
                        "seller_connector_transport_error",
                        "Agentic Commerce seller connector transport failed.").toMap());
        return new AgenticCommerceHttpResponse(
                502,
                AgenticCommerceJson.write(body),
                Map.of(
                        AgenticCommerceProtocol.HEADER_CONTENT_TYPE,
                        AgenticCommerceProtocol.MIME_JSON,
                        AgenticCommerceProtocol.HEADER_REQUEST_ID,
                        request.header(AgenticCommerceProtocol.HEADER_REQUEST_ID)
                                .orElse("req_http_transport_error")),
                responseAttributes(request, startedAt, exception.getClass().getName()));
    }

    private static String joined(List<String> values) {
        return values == null || values.isEmpty() ? "" : String.join(", ", values);
    }

    private static String authorizationHeader(String bearerToken) {
        String normalized = AgenticCommerceWayangMaps.text(bearerToken);
        return normalized.startsWith(AgenticCommerceProtocol.BEARER_PREFIX)
                ? normalized
                : AgenticCommerceProtocol.BEARER_PREFIX + normalized;
    }

    private static long durationMillis(long startedAt) {
        return Math.max(0L, Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
    }

    private static Duration timeout(AgenticCommerceConnectorConfig config) {
        Object raw = AgenticCommerceWayangMaps.first(
                config.attributes(),
                "timeoutMillis",
                "timeoutMs",
                "httpTimeoutMillis");
        long millis = parseLong(raw, DEFAULT_TIMEOUT_MILLIS);
        return Duration.ofMillis(Math.max(1L, millis));
    }

    private static long parseLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = AgenticCommerceWayangMaps.text(value);
        if (text.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}

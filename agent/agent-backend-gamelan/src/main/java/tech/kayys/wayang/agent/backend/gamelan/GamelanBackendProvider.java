package tech.kayys.wayang.agent.backend.gamelan;

import tech.kayys.gamelan.sdk.client.GamelanClient;
import tech.kayys.gamelan.sdk.client.GamelanClientConfig;
import tech.kayys.gamelan.sdk.client.TransportType;
import tech.kayys.wayang.agent.spi.BackendProvider;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.WorkflowBackend;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * ServiceLoader-based backend provider for Gamelan SDK.
 *
 * <p>
 * This provider is automatically discovered when {@code backend-gamelan}
 * is on the classpath. It creates GamelanBackendAdapter instances
 * for workflow management.
 * </p>
 *
 * <h3>Auto-Registration:</h3>
 * <p>
 * Create {@code META-INF/services/tech.kayys.wayang.agent.spi.BackendProvider} with:
 * <pre>
 * tech.kayys.wayang.agent.backend.gamelan.GamelanBackendProvider
 * </pre>
 * </p>
 *
 * @author Wayang Team
 * @version 1.0.0
 * @since 2026-04-06
 */
public class GamelanBackendProvider implements BackendProvider {

    private static final boolean SDK_AVAILABLE = classAvailable("tech.kayys.gamelan.sdk.client.GamelanClient");

    @Override
    public String name() {
        return "gamelan";
    }

    @Override
    public int priority() {
        return 100;  // High priority - Gamelan is the primary workflow backend
    }

    @Override
    public InferenceBackend createInferenceBackend(Map<String, Object> config) {
        // Gamelan is workflow-only, no inference backend
        return null;
    }

    @Override
    public WorkflowBackend createWorkflowBackend(Map<String, Object> config) {
        if (!SDK_AVAILABLE) {
            throw new IllegalStateException(
                "Gamelan SDK not available. Add gamelan-sdk-client-core to classpath."
            );
        }

        GamelanClientConfig.Builder builder = GamelanClientConfig.builder();
        Map<String, Object> effectiveConfig = config == null ? Map.of() : config;

        String endpoint = stringValue(effectiveConfig, "endpoint")
                .or(() -> legacyEndpoint(effectiveConfig))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Gamelan backend requires an endpoint config value"));
        String tenantId = stringValue(effectiveConfig, "tenantId")
                .orElseThrow(() -> new IllegalArgumentException(
                        "Gamelan backend requires a tenantId config value"));

        builder.endpoint(endpoint);
        builder.tenantId(tenantId);
        stringValue(effectiveConfig, "apiKey").ifPresent(builder::apiKey);
        durationValue(effectiveConfig, "timeout").ifPresent(builder::timeout);
        transportValue(effectiveConfig, "transport").ifPresent(builder::transport);
        headersValue(effectiveConfig).forEach(builder::header);

        GamelanClientConfig clientConfig = builder.build();
        GamelanClient client = GamelanClient.create(clientConfig);

        return new GamelanBackendAdapter(client);
    }

    @Override
    public boolean isAvailable() {
        return SDK_AVAILABLE;
    }

    @Override
    public List<String> supportedBackends() {
        return List.of("workflow");
    }

    private static boolean classAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static Optional<String> stringValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value instanceof String text && !text.isBlank()
                ? Optional.of(text)
                : Optional.empty();
    }

    private static Optional<Duration> durationValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Duration duration) {
            return Optional.of(duration);
        }
        if (value instanceof Number millis) {
            return Optional.of(Duration.ofMillis(millis.longValue()));
        }
        if (value instanceof String text && !text.isBlank()) {
            return Optional.of(Duration.parse(text));
        }
        return Optional.empty();
    }

    private static Optional<TransportType> transportValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof TransportType transport) {
            return Optional.of(transport);
        }
        if (value instanceof String text && !text.isBlank()) {
            return Optional.of(TransportType.valueOf(text.trim().toUpperCase(Locale.ROOT)));
        }
        return Optional.empty();
    }

    private static Map<String, String> headersValue(Map<String, Object> config) {
        Object value = config.get("headers");
        if (!(value instanceof Map<?, ?> headers)) {
            return Map.of();
        }
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().toString(),
                        entry -> entry.getValue().toString()));
    }

    private static Optional<String> legacyEndpoint(Map<String, Object> config) {
        Optional<String> host = stringValue(config, "host");
        if (host.isEmpty()) {
            return Optional.empty();
        }
        int port = config.get("port") instanceof Number number ? number.intValue() : 8080;
        String scheme = stringValue(config, "scheme").orElse("http");
        return Optional.of(scheme + "://" + host.get() + ":" + port);
    }
}

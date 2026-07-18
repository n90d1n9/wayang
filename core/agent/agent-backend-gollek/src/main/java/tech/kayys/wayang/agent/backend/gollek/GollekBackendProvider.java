package tech.kayys.wayang.agent.backend.gollek;

import tech.kayys.gollek.factory.GollekSdkFactory;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.wayang.agent.spi.BackendProvider;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.WorkflowBackend;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * ServiceLoader-based backend provider for Gollek SDK.
 *
 * <p>
 * This provider is automatically discovered when {@code backend-gollek}
 * is on the classpath. It creates GollekBackendAdapter instances
 * for inference and can optionally create workflow adapters.
 * </p>
 *
 * <h3>Auto-Registration:</h3>
 * <p>
 * Create {@code META-INF/services/tech.kayys.wayang.agent.spi.BackendProvider} with:
 * <pre>
 * tech.kayys.wayang.agent.backend.gollek.GollekBackendProvider
 * </pre>
 * </p>
 *
 * @author Wayang Team
 * @version 0.1.0
 * @since 2026-04-06
 */
public class GollekBackendProvider implements BackendProvider {

    private static final String SDK_CLASS = "tech.kayys.gollek.sdk.core.GollekSdk";
    private static final String SDK_FACTORY_CLASS = "tech.kayys.gollek.factory.GollekSdkFactory";
    private static final boolean SDK_AVAILABLE = classAvailable(SDK_CLASS) && classAvailable(SDK_FACTORY_CLASS);

    private static boolean classAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public String name() {
        return "gollek";
    }

    @Override
    public int priority() {
        return 100;  // High priority - Gollek is the primary backend
    }

    @Override
    public InferenceBackend createInferenceBackend(Map<String, Object> config) {
        if (!SDK_AVAILABLE) {
            throw new IllegalStateException(
                "Gollek SDK not available. Add gollek-sdk to classpath."
            );
        }

        try {
            GollekSdkFactory.Builder builder = GollekSdkFactory.builder();

            stringValue(config, "baseUrl").ifPresent(builder::baseUrl);
            stringValue(config, "apiKey").ifPresent(builder::apiKey);
            stringValue(config, "preferredProvider").ifPresent(builder::preferredProvider);
            durationValue(config, "timeout").ifPresent(builder::requestTimeout);
            intValue(config, "retryAttempts").ifPresent(builder::maxRetries);
            booleanValue(config, "enableMetrics").ifPresent(builder::enableMetrics);

            GollekSdk sdk = builder.build();
            return new GollekBackendAdapter(sdk);
        } catch (SdkException e) {
            throw new IllegalStateException("Failed to create Gollek SDK backend", e);
        }
    }

    private java.util.Optional<String> stringValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value instanceof String text && !text.isBlank()
                ? java.util.Optional.of(text)
                : java.util.Optional.empty();
    }

    private java.util.Optional<Duration> durationValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Duration duration) {
            return java.util.Optional.of(duration);
        }
        if (value instanceof Number seconds) {
            return java.util.Optional.of(Duration.ofSeconds(seconds.longValue()));
        }
        return java.util.Optional.empty();
    }

    private java.util.Optional<Integer> intValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value instanceof Number number
                ? java.util.Optional.of(number.intValue())
                : java.util.Optional.empty();
    }

    private java.util.Optional<Boolean> booleanValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Boolean bool) {
            return java.util.Optional.of(bool);
        }
        if (value instanceof String text) {
            if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
                return java.util.Optional.of(Boolean.parseBoolean(text));
            }
        }
        return java.util.Optional.empty();
    }

    @Override
    public WorkflowBackend createWorkflowBackend(Map<String, Object> config) {
        // Gollek is inference-only, no workflow backend
        return null;
    }

    @Override
    public boolean isAvailable() {
        return SDK_AVAILABLE;
    }

    @Override
    public List<String> supportedBackends() {
        return List.of("inference");
    }
}

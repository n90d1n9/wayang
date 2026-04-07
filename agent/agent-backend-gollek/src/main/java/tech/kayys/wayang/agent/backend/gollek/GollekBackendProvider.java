package tech.kayys.wayang.agent.backend.gollek;

import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.wayang.agent.spi.BackendProvider;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.WorkflowBackend;

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
 * @version 1.0.0
 * @since 2026-04-06
 */
public class GollekBackendProvider implements BackendProvider {

    private static volatile boolean sdkAvailable = false;

    static {
        // Check if Gollek SDK is on classpath
        try {
            Class.forName("tech.kayys.gollek.sdk.core.GollekSdk");
            sdkAvailable = true;
        } catch (ClassNotFoundException e) {
            sdkAvailable = false;
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
        if (!sdkAvailable) {
            throw new IllegalStateException(
                "Gollek SDK not available. Add gollek-sdk to classpath."
            );
        }

        // Build Gollek SDK with configuration
        GollekSdk.Builder builder = GollekSdk.builder();

        // Apply configuration if present
        if (config.containsKey("preferredProvider")) {
            builder.preferredProvider((String) config.get("preferredProvider"));
        }
        if (config.containsKey("timeout")) {
            Object timeout = config.get("timeout");
            if (timeout instanceof java.time.Duration d) {
                builder.defaultTimeout(d);
            }
        }
        if (config.containsKey("retryAttempts")) {
            Object retries = config.get("retryAttempts");
            if (retries instanceof Number n) {
                builder.maxRetries(n.intValue());
            }
        }

        GollekSdk sdk = builder.build();

        return new GollekBackendAdapter(sdk);
    }

    @Override
    public WorkflowBackend createWorkflowBackend(Map<String, Object> config) {
        // Gollek is inference-only, no workflow backend
        return null;
    }

    @Override
    public boolean isAvailable() {
        return sdkAvailable;
    }

    @Override
    public List<String> supportedBackends() {
        return List.of("inference");
    }
}

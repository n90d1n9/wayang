package tech.kayys.wayang.agent.backend.gamelan;

import tech.kayys.gamelan.sdk.client.GamelanClient;
import tech.kayys.gamelan.sdk.client.GamelanClientConfig;
import tech.kayys.wayang.agent.spi.BackendProvider;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.WorkflowBackend;

import java.util.List;
import java.util.Map;

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

    private static volatile boolean sdkAvailable = false;

    static {
        // Check if Gamelan SDK is on classpath
        try {
            Class.forName("tech.kayys.gamelan.sdk.client.GamelanClient");
            sdkAvailable = true;
        } catch (ClassNotFoundException e) {
            sdkAvailable = false;
        }
    }

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
        if (!sdkAvailable) {
            throw new IllegalStateException(
                "Gamelan SDK not available. Add gamelan-sdk-client-core to classpath."
            );
        }

        // Build Gamelan Client with configuration
        GamelanClientConfig.Builder builder = GamelanClientConfig.builder();

        // Apply configuration if present
        if (config.containsKey("transport")) {
            Object transport = config.get("transport");
            if (transport instanceof String t) {
                try {
                    builder.transport(tech.kayys.gamelan.sdk.client.TransportType.valueOf(t));
                } catch (IllegalArgumentException e) {
                    // Use default
                }
            }
        }
        if (config.containsKey("host")) {
            builder.host((String) config.get("host"));
        }
        if (config.containsKey("port")) {
            Object port = config.get("port");
            if (port instanceof Number n) {
                builder.port(n.intValue());
            }
        }

        GamelanClientConfig clientConfig = builder.build();
        GamelanClient client = GamelanClient.create(clientConfig);

        return new GamelanBackendAdapter(client);
    }

    @Override
    public boolean isAvailable() {
        return sdkAvailable;
    }

    @Override
    public List<String> supportedBackends() {
        return List.of("workflow");
    }
}

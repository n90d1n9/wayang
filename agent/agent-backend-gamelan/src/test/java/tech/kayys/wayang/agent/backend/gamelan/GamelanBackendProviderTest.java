package tech.kayys.wayang.agent.backend.gamelan;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GamelanBackendProviderTest {

    @Test
    void validatesRequiredSdkConfigurationBeforeCreatingClient() {
        GamelanBackendProvider provider = new GamelanBackendProvider();

        assertThatThrownBy(() -> provider.createWorkflowBackend(Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endpoint");

        assertThatThrownBy(() -> provider.createWorkflowBackend(Map.of("endpoint", "http://localhost:8080")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }
}

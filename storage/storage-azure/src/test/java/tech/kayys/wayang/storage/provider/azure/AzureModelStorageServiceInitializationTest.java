package tech.kayys.wayang.storage.provider.azure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AzureModelStorageServiceInitializationTest {

    @Test
    void modelStorageOperationsRequireInitialization() {
        AzureModelStorageService service = new AzureModelStorageService();

        assertThatThrownBy(() -> service.uploadModel("agent", "planner", "v1", new byte[0]).await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Azure model storage service has not been initialized");
        assertThatThrownBy(() -> service.downloadModel("azure://wayang-models/models/agent/planner/v1")
                .await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Azure model storage service has not been initialized");
        assertThatThrownBy(() -> service.deleteModel("azure://wayang-models/models/agent/planner/v1")
                .await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Azure model storage service has not been initialized");
    }
}

package tech.kayys.wayang.storage.provider.gcs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GcsModelStorageServiceInitializationTest {

    @Test
    void modelStorageOperationsRequireInitialization() {
        GcsModelStorageService service = new GcsModelStorageService();

        assertThatThrownBy(() -> service.uploadModel("agent", "planner", "v1", new byte[0]).await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("GCS model storage service has not been initialized");
        assertThatThrownBy(() -> service.downloadModel("gs://wayang-models/models/agent/planner/v1")
                .await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("GCS model storage service has not been initialized");
        assertThatThrownBy(() -> service.deleteModel("gs://wayang-models/models/agent/planner/v1")
                .await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("GCS model storage service has not been initialized");
    }
}

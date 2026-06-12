package tech.kayys.wayang.storage.provider.s3;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3StorageServiceInitializationTest {

    @Test
    void objectStorageOperationsRequireInitialization() {
        S3ObjectStorageService service = new S3ObjectStorageService();

        assertThatThrownBy(() -> service.getObject("skills/definitions/planner.properties").await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("S3 object storage service has not been initialized");
        assertThatThrownBy(() -> service.listObjects("skills/").await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("S3 object storage service has not been initialized");
        assertThatThrownBy(() -> service.putObject("skills/definitions/planner.properties", new byte[0])
                .await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("S3 object storage service has not been initialized");
        assertThatThrownBy(() -> service.deleteObject("skills/definitions/planner.properties").await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("S3 object storage service has not been initialized");
    }

    @Test
    void modelStorageOperationsRequireInitialization() {
        S3ModelStorageService service = new S3ModelStorageService();

        assertThatThrownBy(() -> service.uploadModel("agent", "planner", "v1", new byte[0]).await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("S3 model storage service has not been initialized");
        assertThatThrownBy(() -> service.downloadModel("s3://wayang-models/models/agent/planner/v1")
                .await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("S3 model storage service has not been initialized");
        assertThatThrownBy(() -> service.deleteModel("s3://wayang-models/models/agent/planner/v1")
                .await().indefinitely())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("S3 model storage service has not been initialized");
    }
}

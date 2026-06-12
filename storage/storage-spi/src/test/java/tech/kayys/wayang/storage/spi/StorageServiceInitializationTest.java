package tech.kayys.wayang.storage.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageServiceInitializationTest {

    @Test
    void returnsInitializedValues() {
        Object marker = new Object();

        assertThat(StorageServiceInitialization.requireInitialized(marker, "Test storage service"))
                .isSameAs(marker);
        assertThat(StorageServiceInitialization.requireTextInitialized("bucket", "Test storage service"))
                .isEqualTo("bucket");
    }

    @Test
    void rejectsMissingInitializedValuesWithServiceName() {
        assertThatThrownBy(() -> StorageServiceInitialization.requireInitialized(null, "S3 object storage service"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("S3 object storage service has not been initialized");
        assertThatThrownBy(() -> StorageServiceInitialization.requireTextInitialized(" ", "GCS model storage service"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("GCS model storage service has not been initialized");
    }
}

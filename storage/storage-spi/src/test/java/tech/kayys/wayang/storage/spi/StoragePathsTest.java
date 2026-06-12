package tech.kayys.wayang.storage.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoragePathsTest {

    @Test
    void normalizesBlankAndConfiguredModelPrefixes() {
        assertThat(StoragePaths.normalizePrefix(null)).isEqualTo("models/");
        assertThat(StoragePaths.normalizePrefix(" ")).isEqualTo("models/");
        assertThat(StoragePaths.normalizePrefix("///")).isEqualTo("models/");
        assertThat(StoragePaths.normalizePrefix(" /tenant-a/models "))
                .isEqualTo("tenant-a/models/");
        assertThat(StoragePaths.normalizePrefix("tenant-a/models/"))
                .isEqualTo("tenant-a/models/");
    }

    @Test
    void buildsModelObjectNameFromRequiredSegments() {
        assertThat(StoragePaths.modelObjectName(" /tenant-a/models ", " agents ", " planner ", " v1 "))
                .isEqualTo("tenant-a/models/agents/planner/v1");
    }

    @Test
    void rendersStorageUriWithNormalizedComponents() {
        assertThat(StoragePaths.storageUri(" s3 ", " wayang-models ", " /models/agent/planner/v1 "))
                .isEqualTo("s3://wayang-models/models/agent/planner/v1");
    }

    @Test
    void extractsObjectNameFromTrimmedOwnedUri() {
        assertThat(StoragePaths.objectNameFromUri(
                " s3://wayang-models/models/agent/planner/v1 ",
                " s3 ",
                " wayang-models "))
                .isEqualTo("models/agent/planner/v1");
    }

    @Test
    void rejectsBlankRequiredSegmentsAndForeignUris() {
        assertThatThrownBy(() -> StoragePaths.modelObjectName("models", " ", "planner", "v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("namespace must not be blank");
        assertThatThrownBy(() -> StoragePaths.storageUri("s3", "wayang-models", "///"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("objectName must not be blank");
        assertThatThrownBy(() -> StoragePaths.objectNameFromUri(
                "gs://wayang-models/models/agent/planner/v1",
                "s3",
                "wayang-models"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Storage URI must start with s3://wayang-models/");
    }
}

package tech.kayys.wayang.storage.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelStorageObjectsTest {

    @Test
    void mapsModelCoordinatesThroughProviderContainer() {
        ModelStorageObjects objects = ModelStorageObjects.forContainer(
                " s3 ",
                " wayang-models ",
                " /tenants/acme/models ");

        String objectName = objects.objectName(" agents ", " planner ", " v1 ");

        assertThat(objects.scheme()).isEqualTo("s3");
        assertThat(objects.containerName()).isEqualTo("wayang-models");
        assertThat(objects.pathPrefix()).isEqualTo("tenants/acme/models/");
        assertThat(objectName).isEqualTo("tenants/acme/models/agents/planner/v1");
        assertThat(objects.storageUri(objectName))
                .isEqualTo("s3://wayang-models/tenants/acme/models/agents/planner/v1");
    }

    @Test
    void defaultsBlankPrefixToModelPrefix() {
        ModelStorageObjects objects = ModelStorageObjects.forContainer("gs", "wayang-models", " ");

        assertThat(objects.pathPrefix()).isEqualTo("models/");
        assertThat(objects.objectName("agent", "planner", "v1"))
                .isEqualTo("models/agent/planner/v1");
    }

    @Test
    void extractsObjectNameFromOwnedUri() {
        ModelStorageObjects objects = ModelStorageObjects.forContainer("azure", "wayang-models", "models");

        assertThat(objects.objectNameFromUri(" azure://wayang-models/models/agent/planner/v1 "))
                .isEqualTo("models/agent/planner/v1");
    }

    @Test
    void rejectsBlankContainerAndForeignUris() {
        assertThatThrownBy(() -> ModelStorageObjects.forContainer("s3", " ", "models"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("containerName must not be blank");

        ModelStorageObjects objects = ModelStorageObjects.forContainer("s3", "wayang-models", "models");

        assertThatThrownBy(() -> objects.objectNameFromUri("s3://other/models/agent/planner/v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Storage URI must start with s3://wayang-models/");
    }
}

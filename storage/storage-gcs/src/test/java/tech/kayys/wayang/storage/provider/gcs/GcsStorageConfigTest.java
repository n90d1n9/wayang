package tech.kayys.wayang.storage.provider.gcs;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GcsStorageConfigTest {

    @Test
    void normalizesOptionalValuesAndRequiresBucket() {
        GcsStorageConfig config = GcsStorageConfig.of(
                " wayang-models ",
                " wayang-project ",
                " tenants/acme ");

        assertThat(config.bucketName()).isEqualTo("wayang-models");
        assertThat(config.projectId()).isEqualTo("wayang-project");
        assertThat(config.pathPrefix()).isEqualTo("tenants/acme");

        assertThatThrownBy(() -> GcsStorageConfig.of(" ", "project", "models"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bucketName must not be blank");
    }

    @Test
    void parsesPropertyBackedConfiguration() {
        Properties properties = new Properties();
        properties.setProperty(GcsStorageConfig.BUCKET_PROPERTY, "wayang-models");
        properties.setProperty(GcsStorageConfig.PROJECT_ID_PROPERTY, "wayang-project");
        properties.setProperty(GcsStorageConfig.PATH_PREFIX_PROPERTY, "tenants/acme");

        GcsStorageConfig config = GcsStorageConfig.fromProperties(properties);

        assertThat(config.bucketName()).isEqualTo("wayang-models");
        assertThat(config.projectId()).isEqualTo("wayang-project");
        assertThat(config.pathPrefix()).isEqualTo("tenants/acme");
    }

    @Test
    void parsesEnvironmentBackedConfiguration() {
        GcsStorageConfig config = GcsStorageConfig.fromEnvironment(Map.of(
                GcsStorageConfig.BUCKET_ENV, "wayang-models",
                GcsStorageConfig.PROJECT_ID_ENV, "wayang-project",
                GcsStorageConfig.PATH_PREFIX_ENV, "tenant-a"));

        assertThat(config.bucketName()).isEqualTo("wayang-models");
        assertThat(config.projectId()).isEqualTo("wayang-project");
        assertThat(config.pathPrefix()).isEqualTo("tenant-a");
    }

    @Test
    void defaultsOptionalValues() {
        GcsStorageConfig config = GcsStorageConfig.fromMap(Map.of(
                GcsStorageConfig.BUCKET_PROPERTY, "wayang-models"));

        assertThat(config.projectId()).isEmpty();
        assertThat(config.pathPrefix()).isEmpty();
    }
}

package tech.kayys.wayang.storage.provider.azure;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AzureStorageConfigTest {

    @Test
    void normalizesOptionalValuesAndRequiresCoreFields() {
        AzureStorageConfig config = AzureStorageConfig.of(
                " AccountName=wayang;AccountKey=secret ",
                " wayang-models ",
                " tenants/acme ");

        assertThat(config.connectionString()).isEqualTo("AccountName=wayang;AccountKey=secret");
        assertThat(config.containerName()).isEqualTo("wayang-models");
        assertThat(config.pathPrefix()).isEqualTo("tenants/acme");

        assertThatThrownBy(() -> AzureStorageConfig.of(" ", "wayang-models", "models"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("connectionString must not be blank");
        assertThatThrownBy(() -> AzureStorageConfig.of("connection", " ", "models"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("containerName must not be blank");
    }

    @Test
    void parsesPropertyBackedConfiguration() {
        Properties properties = new Properties();
        properties.setProperty(AzureStorageConfig.CONNECTION_STRING_PROPERTY, "connection");
        properties.setProperty(AzureStorageConfig.CONTAINER_PROPERTY, "wayang-models");
        properties.setProperty(AzureStorageConfig.PATH_PREFIX_PROPERTY, "tenants/acme");

        AzureStorageConfig config = AzureStorageConfig.fromProperties(properties);

        assertThat(config.connectionString()).isEqualTo("connection");
        assertThat(config.containerName()).isEqualTo("wayang-models");
        assertThat(config.pathPrefix()).isEqualTo("tenants/acme");
    }

    @Test
    void parsesEnvironmentBackedConfiguration() {
        AzureStorageConfig config = AzureStorageConfig.fromEnvironment(Map.of(
                AzureStorageConfig.CONNECTION_STRING_ENV, "connection",
                AzureStorageConfig.CONTAINER_ENV, "wayang-models",
                AzureStorageConfig.PATH_PREFIX_ENV, "tenant-a"));

        assertThat(config.connectionString()).isEqualTo("connection");
        assertThat(config.containerName()).isEqualTo("wayang-models");
        assertThat(config.pathPrefix()).isEqualTo("tenant-a");
    }

    @Test
    void defaultsOptionalValues() {
        AzureStorageConfig config = AzureStorageConfig.fromMap(Map.of(
                AzureStorageConfig.CONNECTION_STRING_PROPERTY, "connection",
                AzureStorageConfig.CONTAINER_PROPERTY, "wayang-models"));

        assertThat(config.pathPrefix()).isEmpty();
    }
}

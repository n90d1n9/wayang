package tech.kayys.wayang.storage.provider.s3;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3StorageConfigTest {

    @Test
    void normalizesOptionalValuesAndRequiresCoreFields() {
        S3StorageConfig config = S3StorageConfig.of(
                " access ",
                " secret ",
                " bucket ",
                " us-east-1 ",
                " ",
                " tenants/acme ",
                true);

        assertThat(config.accessKeyId()).isEqualTo("access");
        assertThat(config.secretAccessKey()).isEqualTo("secret");
        assertThat(config.bucketName()).isEqualTo("bucket");
        assertThat(config.region()).isEqualTo("us-east-1");
        assertThat(config.endpoint()).isEmpty();
        assertThat(config.pathPrefix()).isEqualTo("tenants/acme");
        assertThat(config.pathStyleAccess()).isTrue();

        assertThatThrownBy(() -> S3StorageConfig.of("", "secret", "bucket", "us-east-1", "", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("accessKeyId must not be blank");
    }

    @Test
    void parsesPropertyBackedConfiguration() {
        Properties properties = new Properties();
        properties.setProperty(S3StorageConfig.ACCESS_KEY_ID_PROPERTY, "access");
        properties.setProperty(S3StorageConfig.SECRET_ACCESS_KEY_PROPERTY, "secret");
        properties.setProperty(S3StorageConfig.BUCKET_PROPERTY, "wayang");
        properties.setProperty(S3StorageConfig.REGION_PROPERTY, "ap-southeast-3");
        properties.setProperty(S3StorageConfig.ENDPOINT_PROPERTY, "http://localhost:9000");
        properties.setProperty(S3StorageConfig.PATH_PREFIX_PROPERTY, "tenants/acme");
        properties.setProperty(S3StorageConfig.PATH_STYLE_ACCESS_PROPERTY, "true");

        S3StorageConfig config = S3StorageConfig.fromProperties(properties);

        assertThat(config.accessKeyId()).isEqualTo("access");
        assertThat(config.secretAccessKey()).isEqualTo("secret");
        assertThat(config.bucketName()).isEqualTo("wayang");
        assertThat(config.region()).isEqualTo("ap-southeast-3");
        assertThat(config.endpoint()).isEqualTo("http://localhost:9000");
        assertThat(config.pathPrefix()).isEqualTo("tenants/acme");
        assertThat(config.pathStyleAccess()).isTrue();
    }

    @Test
    void parsesEnvironmentBackedConfiguration() {
        S3StorageConfig config = S3StorageConfig.fromEnvironment(Map.of(
                S3StorageConfig.ACCESS_KEY_ID_ENV, "access",
                S3StorageConfig.SECRET_ACCESS_KEY_ENV, "secret",
                S3StorageConfig.BUCKET_ENV, "wayang",
                S3StorageConfig.REGION_ENV, "us-east-1",
                S3StorageConfig.ENDPOINT_ENV, "http://rustfs.local:9000",
                S3StorageConfig.PATH_PREFIX_ENV, "tenant-a",
                S3StorageConfig.PATH_STYLE_ACCESS_ENV, "true"));

        assertThat(config.endpoint()).isEqualTo("http://rustfs.local:9000");
        assertThat(config.pathPrefix()).isEqualTo("tenant-a");
        assertThat(config.pathStyleAccess()).isTrue();
    }

    @Test
    void defaultsOptionalConnectionValues() {
        S3StorageConfig config = S3StorageConfig.fromMap(Map.of(
                S3StorageConfig.ACCESS_KEY_ID_PROPERTY, "access",
                S3StorageConfig.SECRET_ACCESS_KEY_PROPERTY, "secret",
                S3StorageConfig.BUCKET_PROPERTY, "wayang",
                S3StorageConfig.REGION_PROPERTY, "us-east-1"));

        assertThat(config.endpoint()).isEmpty();
        assertThat(config.pathPrefix()).isEmpty();
        assertThat(config.pathStyleAccess()).isFalse();
    }
}

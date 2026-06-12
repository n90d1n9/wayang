package tech.kayys.wayang.storage.provider.s3;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class S3StorageClientsTest {

    @Test
    void createsClientFromTypedConfig() {
        S3Client client = S3StorageClients.create(S3StorageConfig.of(
                "access",
                "secret",
                "wayang",
                "us-east-1",
                "http://localhost:9000",
                "tenants/acme",
                true));

        try {
            assertThat(client).isNotNull();
        } finally {
            client.close();
        }
    }

    @Test
    void requiresConfig() {
        assertThatNullPointerException()
                .isThrownBy(() -> S3StorageClients.create(null))
                .withMessage("config");
    }
}

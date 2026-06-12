package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticCommerceObjectStoreClientRegistryTest {

    @Test
    void resolvesClientsByMostSpecificLocationFirst() {
        InMemoryAgenticCommerceObjectStoreClient providerClient = InMemoryAgenticCommerceObjectStoreClient.create();
        InMemoryAgenticCommerceObjectStoreClient bucketClient = InMemoryAgenticCommerceObjectStoreClient.create();
        InMemoryAgenticCommerceObjectStoreClient exactClient = InMemoryAgenticCommerceObjectStoreClient.create();
        InMemoryAgenticCommerceObjectStoreClient defaultClient = InMemoryAgenticCommerceObjectStoreClient.create();
        AgenticCommerceObjectStoreConfig exactConfig = config("s3", "https://s3.example", "state-a");
        AgenticCommerceObjectStoreClientRegistry registry = AgenticCommerceObjectStoreClientRegistry.builder()
                .defaultClient(defaultClient)
                .provider("s3", providerClient)
                .bucket("state-a", bucketClient)
                .location(exactConfig, exactClient)
                .build();

        assertThat(registry.resolve(exactConfig)).isSameAs(exactClient);
        assertThat(registry.resolve(config("s3", "https://other.example", "state-a"))).isSameAs(bucketClient);
        assertThat(registry.resolve(config("s3", "https://other.example", "state-b"))).isSameAs(providerClient);
        assertThat(registry.resolve(config("rustfs", "https://rustfs.example", "state-c"))).isSameAs(defaultClient);
        assertThat(registry.toMap())
                .containsEntry("registeredClientCount", 3)
                .containsEntry("defaultClientConfigured", true);
    }

    @Test
    void resolvesProviderBucketBeforeProvider() {
        InMemoryAgenticCommerceObjectStoreClient providerClient = InMemoryAgenticCommerceObjectStoreClient.create();
        InMemoryAgenticCommerceObjectStoreClient providerBucketClient = InMemoryAgenticCommerceObjectStoreClient.create();
        AgenticCommerceObjectStoreClientRegistry registry = AgenticCommerceObjectStoreClientRegistry.builder()
                .provider("rustfs", providerClient)
                .providerBucket("rustfs", "tenant-state", providerBucketClient)
                .build();

        assertThat(registry.resolve(config("rustfs", "https://rustfs.example", "tenant-state")))
                .isSameAs(providerBucketClient);
        assertThat(registry.resolve(config("rustfs", "https://rustfs.example", "other-state")))
                .isSameAs(providerClient);
    }

    @Test
    void throwsWhenNoClientMatches() {
        AgenticCommerceObjectStoreClientRegistry registry = AgenticCommerceObjectStoreClientRegistry.builder()
                .provider("s3", InMemoryAgenticCommerceObjectStoreClient.create())
                .build();

        assertThatThrownBy(() -> registry.resolve(config("rustfs", "https://rustfs.example", "state")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No Agentic Commerce object-store client registered");
    }

    private static AgenticCommerceObjectStoreConfig config(String provider, String endpoint, String bucket) {
        return AgenticCommerceObjectStoreConfig.fromMap(Map.of(
                "provider",
                provider,
                "endpoint",
                endpoint,
                "bucket",
                bucket));
    }
}

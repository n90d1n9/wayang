package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic object-store client resolver for multi-backend persistence.
 */
public final class AgenticCommerceObjectStoreClientRegistry implements AgenticCommerceObjectStoreClientResolver {

    private final Map<String, AgenticCommerceObjectStoreClient> clients;
    private final AgenticCommerceObjectStoreClient defaultClient;

    private AgenticCommerceObjectStoreClientRegistry(
            Map<String, AgenticCommerceObjectStoreClient> clients,
            AgenticCommerceObjectStoreClient defaultClient) {
        this.clients = Map.copyOf(clients);
        this.defaultClient = defaultClient;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AgenticCommerceObjectStoreClientRegistry fixed(AgenticCommerceObjectStoreClient client) {
        return builder().defaultClient(client).build();
    }

    @Override
    public AgenticCommerceObjectStoreClient resolve(AgenticCommerceObjectStoreConfig config) {
        AgenticCommerceObjectStoreConfig resolved = Objects.requireNonNull(config, "config");
        for (String key : resolutionKeys(resolved)) {
            AgenticCommerceObjectStoreClient client = clients.get(key);
            if (client != null) {
                return client;
            }
        }
        if (defaultClient != null) {
            return defaultClient;
        }
        throw new IllegalArgumentException(
                "No Agentic Commerce object-store client registered for " + locationLabel(resolved));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("registeredClientCount", clients.size());
        values.put("defaultClientConfigured", defaultClient != null);
        values.put("registeredKeys", clients.keySet().stream().sorted().toList());
        return Map.copyOf(values);
    }

    private static String[] resolutionKeys(AgenticCommerceObjectStoreConfig config) {
        String provider = normalize(config.provider());
        String endpoint = normalize(config.endpoint());
        String bucket = normalize(config.bucket());
        return new String[] {
                locationKey(provider, endpoint, bucket),
                providerBucketKey(provider, bucket),
                endpointKey(endpoint),
                bucketKey(bucket),
                providerKey(provider)
        };
    }

    private static String locationLabel(AgenticCommerceObjectStoreConfig config) {
        return "provider="
                + config.provider()
                + ", endpoint="
                + config.endpoint()
                + ", bucket="
                + config.bucket();
    }

    private static String providerKey(String provider) {
        return "provider:" + normalize(provider);
    }

    private static String endpointKey(String endpoint) {
        return "endpoint:" + normalize(endpoint);
    }

    private static String bucketKey(String bucket) {
        return "bucket:" + normalize(bucket);
    }

    private static String providerBucketKey(String provider, String bucket) {
        return "provider-bucket:" + normalize(provider) + "/" + normalize(bucket);
    }

    private static String locationKey(String provider, String endpoint, String bucket) {
        return "location:" + normalize(provider) + "/" + normalize(endpoint) + "/" + normalize(bucket);
    }

    private static String normalize(String value) {
        return AgenticCommerceWayangMaps.text(value).toLowerCase(Locale.ROOT);
    }

    public static final class Builder {

        private final Map<String, AgenticCommerceObjectStoreClient> clients = new LinkedHashMap<>();
        private AgenticCommerceObjectStoreClient defaultClient;

        private Builder() {
        }

        public Builder defaultClient(AgenticCommerceObjectStoreClient client) {
            this.defaultClient = Objects.requireNonNull(client, "client");
            return this;
        }

        public Builder provider(String provider, AgenticCommerceObjectStoreClient client) {
            return put(providerKey(provider), client);
        }

        public Builder endpoint(String endpoint, AgenticCommerceObjectStoreClient client) {
            return put(endpointKey(endpoint), client);
        }

        public Builder bucket(String bucket, AgenticCommerceObjectStoreClient client) {
            return put(bucketKey(bucket), client);
        }

        public Builder providerBucket(
                String provider,
                String bucket,
                AgenticCommerceObjectStoreClient client) {
            return put(providerBucketKey(provider, bucket), client);
        }

        public Builder location(
                AgenticCommerceObjectStoreConfig config,
                AgenticCommerceObjectStoreClient client) {
            AgenticCommerceObjectStoreConfig resolved = Objects.requireNonNull(config, "config");
            return put(locationKey(resolved.provider(), resolved.endpoint(), resolved.bucket()), client);
        }

        public AgenticCommerceObjectStoreClientRegistry build() {
            return new AgenticCommerceObjectStoreClientRegistry(clients, defaultClient);
        }

        private Builder put(String key, AgenticCommerceObjectStoreClient client) {
            clients.put(
                    AgenticCommerceWayangMaps.required(key, "client registry key"),
                    Objects.requireNonNull(client, "client"));
            return this;
        }
    }
}

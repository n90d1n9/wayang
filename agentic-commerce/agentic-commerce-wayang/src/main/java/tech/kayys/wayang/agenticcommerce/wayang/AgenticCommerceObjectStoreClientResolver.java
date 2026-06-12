package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.Objects;

/**
 * Resolves concrete object-store clients for configured S3/RustFS locations.
 */
@FunctionalInterface
public interface AgenticCommerceObjectStoreClientResolver {

    AgenticCommerceObjectStoreClient resolve(AgenticCommerceObjectStoreConfig config);

    static AgenticCommerceObjectStoreClientResolver fixed(AgenticCommerceObjectStoreClient client) {
        AgenticCommerceObjectStoreClient resolved = Objects.requireNonNull(client, "client");
        return config -> resolved;
    }
}

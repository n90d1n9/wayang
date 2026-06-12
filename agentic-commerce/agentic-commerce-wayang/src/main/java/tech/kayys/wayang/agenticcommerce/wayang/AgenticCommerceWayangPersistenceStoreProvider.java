package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.Map;

/**
 * Builds one kind of Agentic Commerce Wayang persistence store.
 */
public interface AgenticCommerceWayangPersistenceStoreProvider {

    String storageKind();

    boolean supports(AgenticCommerceWayangPersistenceConfig config);

    AgenticCommerceWayangPersistenceStore build(AgenticCommerceWayangPersistenceProviderContext context);

    default Map<String, Object> toMap() {
        return Map.of("storageKind", storageKind());
    }
}

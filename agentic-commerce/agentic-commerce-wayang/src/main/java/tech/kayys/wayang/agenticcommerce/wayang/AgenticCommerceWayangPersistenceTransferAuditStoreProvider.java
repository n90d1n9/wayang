package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.Map;

/**
 * Builds one kind of transfer audit sink/store.
 */
public interface AgenticCommerceWayangPersistenceTransferAuditStoreProvider {

    String storageKind();

    boolean supports(AgenticCommerceWayangPersistenceTransferAuditConfig config);

    AgenticCommerceWayangPersistenceTransferAuditSink build(
            AgenticCommerceWayangPersistenceTransferAuditProviderContext context);

    default Map<String, Object> toMap() {
        return Map.of("storageKind", storageKind());
    }
}

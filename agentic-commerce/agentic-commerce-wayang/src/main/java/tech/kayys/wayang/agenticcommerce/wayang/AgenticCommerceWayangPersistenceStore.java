package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.Map;
import java.util.Optional;

/**
 * Persistence boundary for Agentic Commerce Wayang configuration and snapshots.
 */
public interface AgenticCommerceWayangPersistenceStore {

    String storageKind();

    Optional<AgenticCommerceWayangRuntimeConfig> loadRuntimeConfig();

    void saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig runtimeConfig);

    Optional<AgenticCommerceWayangBootstrapConfig> loadBootstrapConfig();

    void saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig bootstrapConfig);

    Optional<Map<String, Object>> loadBootstrapReport();

    void saveBootstrapReport(AgenticCommerceWayangBootstrapReport bootstrapReport);

    Optional<Map<String, Object>> loadManifest();

    void saveManifest(AgenticCommerceWayangManifest manifest);

    Map<String, Object> toMap();
}

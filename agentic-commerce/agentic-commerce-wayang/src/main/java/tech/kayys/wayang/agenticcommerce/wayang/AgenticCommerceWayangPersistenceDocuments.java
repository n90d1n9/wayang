package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Catalog of persisted Agentic Commerce Wayang documents.
 */
public final class AgenticCommerceWayangPersistenceDocuments {

    public static final AgenticCommerceWayangPersistenceDocument RUNTIME_CONFIG =
            new AgenticCommerceWayangPersistenceDocument(
                    "runtimeConfig",
                    "runtime-config.json",
                    "runtimeConfigPath",
                    "runtimeConfigKey",
                    "runtimeConfigAvailable",
                    "runtime_config_missing",
                    "runtime_config_load_failed");

    public static final AgenticCommerceWayangPersistenceDocument BOOTSTRAP_CONFIG =
            new AgenticCommerceWayangPersistenceDocument(
                    "bootstrapConfig",
                    "bootstrap-config.json",
                    "bootstrapConfigPath",
                    "bootstrapConfigKey",
                    "bootstrapConfigAvailable",
                    "bootstrap_config_missing",
                    "bootstrap_config_load_failed");

    public static final AgenticCommerceWayangPersistenceDocument BOOTSTRAP_REPORT =
            new AgenticCommerceWayangPersistenceDocument(
                    "bootstrapReport",
                    "bootstrap-report.json",
                    "bootstrapReportPath",
                    "bootstrapReportKey",
                    "bootstrapReportAvailable",
                    "bootstrap_report_missing",
                    "bootstrap_report_load_failed");

    public static final AgenticCommerceWayangPersistenceDocument MANIFEST =
            new AgenticCommerceWayangPersistenceDocument(
                    "manifest",
                    "manifest.json",
                    "manifestPath",
                    "manifestKey",
                    "manifestAvailable",
                    "manifest_missing",
                    "manifest_load_failed");

    public static final List<AgenticCommerceWayangPersistenceDocument> ALL = List.of(
            RUNTIME_CONFIG,
            BOOTSTRAP_CONFIG,
            BOOTSTRAP_REPORT,
            MANIFEST);

    private AgenticCommerceWayangPersistenceDocuments() {
    }

    public static int count() {
        return ALL.size();
    }

    public static List<String> ids() {
        return ALL.stream().map(AgenticCommerceWayangPersistenceDocument::id).toList();
    }

    public static List<String> fileNames() {
        return ALL.stream().map(AgenticCommerceWayangPersistenceDocument::fileName).toList();
    }

    public static Optional<AgenticCommerceWayangPersistenceDocument> find(String id) {
        String normalized = AgenticCommerceWayangMaps.text(id);
        return ALL.stream()
                .filter(document -> document.id().equals(normalized))
                .findFirst();
    }

    public static List<Map<String, Object>> toMapList() {
        return ALL.stream().map(AgenticCommerceWayangPersistenceDocument::toMap).toList();
    }
}

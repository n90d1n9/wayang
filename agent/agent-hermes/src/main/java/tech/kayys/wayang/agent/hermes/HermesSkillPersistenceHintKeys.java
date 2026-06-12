package tech.kayys.wayang.agent.hermes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stable config hint keys for learned-skill persistence strategy.
 */
public final class HermesSkillPersistenceHintKeys {

    public static final String DEFINITIONS = HermesSkillPersistenceRouteRoles.DEFINITIONS;
    public static final String ARTIFACTS = HermesSkillPersistenceRouteRoles.ARTIFACTS;
    public static final String FALLBACK = HermesSkillPersistenceRouteRoles.FALLBACK;
    public static final String CLOUD_STORES = "cloudStores";
    public static final String FILE_ROOT = "fileRoot";
    public static final String OBJECT_PREFIX = "objectPrefix";
    public static final String DEFINITION_OBJECT_PREFIX = "definitionObjectPrefix";
    public static final String ARTIFACT_OBJECT_PREFIX = "artifactObjectPrefix";
    public static final String JDBC_DEFINITION_TABLE = "jdbcDefinitionTable";
    public static final String JDBC_ARTIFACT_TABLE = "jdbcArtifactTable";
    public static final String JDBC_INITIALIZE_SCHEMA = "jdbcInitializeSchema";

    private static final List<String> DEFINITION_ALIASES =
            List.of(DEFINITIONS, "definition-store", "definitionStore");
    private static final List<String> ARTIFACT_ALIASES =
            List.of(ARTIFACTS, "artifact-store", "artifactStore");
    private static final List<String> FALLBACK_ALIASES =
            List.of(FALLBACK, "fallback-store", "fallbackStore");
    private static final List<String> CLOUD_STORE_ALIASES =
            List.of("cloud-stores", CLOUD_STORES, "object-stores", "objectStores");
    private static final List<String> FILE_ROOT_ALIASES =
            List.of("file-root", FILE_ROOT, "fileSystemRoot", "file-system-root", "filesystem-root");
    private static final List<String> OBJECT_PREFIX_ALIASES =
            List.of("object-prefix", OBJECT_PREFIX, "objectStoragePrefix", "object-storage-prefix", "cloud-prefix");
    private static final List<String> DEFINITION_OBJECT_PREFIX_ALIASES =
            List.of(
                    "definition-object-prefix",
                    DEFINITION_OBJECT_PREFIX,
                    "definitionsObjectPrefix",
                    "definitionObjectStoragePrefix",
                    "definitions-object-storage-prefix");
    private static final List<String> ARTIFACT_OBJECT_PREFIX_ALIASES =
            List.of(
                    "artifact-object-prefix",
                    ARTIFACT_OBJECT_PREFIX,
                    "artifactsObjectPrefix",
                    "artifactObjectStoragePrefix",
                    "artifacts-object-storage-prefix");
    private static final List<String> JDBC_DEFINITION_TABLE_ALIASES =
            List.of(
                    "jdbc-definition-table",
                    JDBC_DEFINITION_TABLE,
                    "definitionJdbcTable",
                    "definitionsJdbcTable",
                    "definition-database-table");
    private static final List<String> JDBC_ARTIFACT_TABLE_ALIASES =
            List.of(
                    "jdbc-artifact-table",
                    JDBC_ARTIFACT_TABLE,
                    "artifactJdbcTable",
                    "artifactsJdbcTable",
                    "artifact-database-table");
    private static final List<String> JDBC_INITIALIZE_SCHEMA_ALIASES =
            List.of(
                    "jdbc-initialize-schema",
                    JDBC_INITIALIZE_SCHEMA,
                    "databaseInitializeSchema",
                    "initializeJdbcSchema");

    private static final Map<String, String> DEFAULT_HINTS = defaultHintsMap();

    private HermesSkillPersistenceHintKeys() {
    }

    public static Map<String, String> defaultHints() {
        return DEFAULT_HINTS;
    }

    public static String defaultDefinitionStore() {
        return DEFAULT_HINTS.get(DEFINITIONS);
    }

    public static String defaultArtifactStore() {
        return DEFAULT_HINTS.get(ARTIFACTS);
    }

    public static String defaultFallbackStore() {
        return DEFAULT_HINTS.get(FALLBACK);
    }

    public static String definitionStore(Map<String, String> hints) {
        return hint(hints, DEFINITION_ALIASES);
    }

    public static String artifactStore(Map<String, String> hints) {
        return hint(hints, ARTIFACT_ALIASES);
    }

    public static String fallbackStore(Map<String, String> hints) {
        return hint(hints, FALLBACK_ALIASES);
    }

    public static String cloudStores(Map<String, String> hints) {
        return hint(hints, CLOUD_STORE_ALIASES);
    }

    public static String fileRoot(Map<String, String> hints) {
        return hint(hints, FILE_ROOT_ALIASES);
    }

    public static String objectPrefix(Map<String, String> hints) {
        return hint(hints, OBJECT_PREFIX_ALIASES);
    }

    public static String definitionObjectPrefix(Map<String, String> hints) {
        return hint(hints, DEFINITION_OBJECT_PREFIX_ALIASES);
    }

    public static String artifactObjectPrefix(Map<String, String> hints) {
        return hint(hints, ARTIFACT_OBJECT_PREFIX_ALIASES);
    }

    public static String jdbcDefinitionTableName(Map<String, String> hints) {
        return hint(hints, JDBC_DEFINITION_TABLE_ALIASES);
    }

    public static String jdbcArtifactTableName(Map<String, String> hints) {
        return hint(hints, JDBC_ARTIFACT_TABLE_ALIASES);
    }

    public static String jdbcInitializeSchema(Map<String, String> hints) {
        return hint(hints, JDBC_INITIALIZE_SCHEMA_ALIASES);
    }

    public static String hint(Map<String, String> hints, String... keys) {
        return hint(hints, List.of(keys));
    }

    private static String hint(Map<String, String> hints, List<String> keys) {
        if (hints == null || hints.isEmpty()) {
            return "";
        }
        List<String> normalizedKeys = keys.stream()
                .map(HermesConfigValues::normalizeKey)
                .toList();
        String resolved = "";
        for (Map.Entry<String, String> entry : hints.entrySet()) {
            if (normalizedKeys.contains(HermesConfigValues.normalizeKey(entry.getKey()))
                    && entry.getValue() != null
                    && !entry.getValue().isBlank()) {
                resolved = entry.getValue().trim();
            }
        }
        return resolved;
    }

    private static Map<String, String> defaultHintsMap() {
        Map<String, String> hints = new LinkedHashMap<>();
        hints.put(DEFINITIONS, "skill-management.definition-store");
        hints.put(ARTIFACTS, "skill-management.artifact-store");
        hints.put(FALLBACK, "file-system");
        return Collections.unmodifiableMap(hints);
    }
}

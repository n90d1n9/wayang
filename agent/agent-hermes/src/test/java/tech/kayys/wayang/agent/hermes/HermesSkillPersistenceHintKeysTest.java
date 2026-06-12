package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillPersistenceHintKeysTest {

    @Test
    void exposesCanonicalDefaultHints() {
        assertThat(HermesSkillPersistenceHintKeys.defaultHints())
                .containsEntry(
                        HermesSkillPersistenceRouteRoles.DEFINITIONS,
                        "skill-management.definition-store")
                .containsEntry(
                        HermesSkillPersistenceRouteRoles.ARTIFACTS,
                        "skill-management.artifact-store")
                .containsEntry(HermesSkillPersistenceRouteRoles.FALLBACK, "file-system");
    }

    @Test
    void resolvesKnownAliasKeys() {
        Map<String, String> hints = Map.ofEntries(
                Map.entry("definition-store", "database"),
                Map.entry("artifactStore", "s3"),
                Map.entry("FALLBACK_STORE", "local-file"),
                Map.entry("object-stores", "rustfs,minio"),
                Map.entry("file-system-root", "/tmp/hermes/skills"),
                Map.entry("object-storage-prefix", "tenant-a/hermes"),
                Map.entry("definitions-object-storage-prefix", "tenant-a/hermes/defs"),
                Map.entry("artifacts-object-storage-prefix", "tenant-a/hermes/arts"),
                Map.entry("definition-database-table", "hermes_defs"),
                Map.entry("artifact-database-table", "hermes_artifacts"),
                Map.entry("initializeJdbcSchema", "false"));

        assertThat(HermesSkillPersistenceHintKeys.definitionStore(hints)).isEqualTo("database");
        assertThat(HermesSkillPersistenceHintKeys.artifactStore(hints)).isEqualTo("s3");
        assertThat(HermesSkillPersistenceHintKeys.fallbackStore(hints)).isEqualTo("local-file");
        assertThat(HermesSkillPersistenceHintKeys.cloudStores(hints)).isEqualTo("rustfs,minio");
        assertThat(HermesSkillPersistenceHintKeys.fileRoot(hints)).isEqualTo("/tmp/hermes/skills");
        assertThat(HermesSkillPersistenceHintKeys.objectPrefix(hints)).isEqualTo("tenant-a/hermes");
        assertThat(HermesSkillPersistenceHintKeys.definitionObjectPrefix(hints)).isEqualTo("tenant-a/hermes/defs");
        assertThat(HermesSkillPersistenceHintKeys.artifactObjectPrefix(hints)).isEqualTo("tenant-a/hermes/arts");
        assertThat(HermesSkillPersistenceHintKeys.jdbcDefinitionTableName(hints)).isEqualTo("hermes_defs");
        assertThat(HermesSkillPersistenceHintKeys.jdbcArtifactTableName(hints)).isEqualTo("hermes_artifacts");
        assertThat(HermesSkillPersistenceHintKeys.jdbcInitializeSchema(hints)).isEqualTo("false");
    }
}

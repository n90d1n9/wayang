package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillDefinitionStoreSyncConfigsTest {

    @Test
    void parsesMirrorModeFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.definition.sync.mode", "mirror");

        SkillDefinitionStoreSyncOptions options = SkillDefinitionStoreSyncConfigs.fromProperties(properties);

        assertThat(options.overwriteExisting()).isTrue();
        assertThat(options.deleteMissingFromTarget()).isTrue();
        assertThat(options.dryRun()).isFalse();
    }

    @Test
    void parsesDryRunInspectModeFromEnvironment() {
        SkillDefinitionStoreSyncOptions options = SkillDefinitionStoreSyncConfigs.fromEnvironment(Map.of(
                "WAYANG_SKILLS_DEFINITION_SYNC_MODE", "inspect"));

        assertThat(options.overwriteExisting()).isTrue();
        assertThat(options.deleteMissingFromTarget()).isTrue();
        assertThat(options.dryRun()).isTrue();
    }

    @Test
    void booleanOverridesTuneNestedMapMode() {
        SkillDefinitionStoreSyncOptions options = SkillDefinitionStoreSyncConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "skills", Map.of(
                                "definition", Map.of(
                                        "sync", Map.of(
                                                "mode", "mirror",
                                                "delete-missing", "false",
                                                "dry-run", "true"))))));

        assertThat(options.overwriteExisting()).isTrue();
        assertThat(options.deleteMissingFromTarget()).isFalse();
        assertThat(options.dryRun()).isTrue();
    }

    @Test
    void acceptsDefinitionSpecificAliasProfile() {
        SkillDefinitionStoreSyncOptions options = SkillDefinitionStoreSyncConfigs.fromMap(Map.of(
                "wayang.skills.definition.sync.mode", "check",
                "wayang.skills.definition.sync.update-existing", "false",
                "wayang.skills.definition.sync.prune-orphans", "false"));

        assertThat(options.overwriteExisting()).isFalse();
        assertThat(options.deleteMissingFromTarget()).isFalse();
        assertThat(options.dryRun()).isTrue();
    }

    @Test
    void normalizedMapCanApplyOverridesToCallerDefaults() {
        SkillDefinitionStoreSyncOptions options = SkillDefinitionStoreSyncConfigs.fromNormalizedMap(
                SkillStoreConfigValues.flattenAndNormalize(Map.of(
                        "maintenance.definition.sync.delete-missing", "true",
                        "maintenance.definition.sync.dry-run", "true")),
                "maintenance.definition.sync.",
                SkillDefinitionStoreSyncOptions.bootstrap());

        assertThat(options.overwriteExisting()).isFalse();
        assertThat(options.deleteMissingFromTarget()).isTrue();
        assertThat(options.dryRun()).isTrue();
    }

    @Test
    void normalizedMapModeOverridesCallerDefaults() {
        SkillDefinitionStoreSyncOptions options = SkillDefinitionStoreSyncConfigs.fromNormalizedMap(
                SkillStoreConfigValues.flattenAndNormalize(Map.of(
                        "maintenance.definition.sync.mode", "bootstrap")),
                "maintenance.definition.sync.",
                SkillDefinitionStoreSyncOptions.mirror());

        assertThat(options).isEqualTo(SkillDefinitionStoreSyncOptions.bootstrap());
    }

    @Test
    void rejectsUnknownMode() {
        assertThatThrownBy(() -> SkillDefinitionStoreSyncConfigs.fromMap(Map.of(
                "wayang.skills.definition.sync.mode", "mystery")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown definition sync mode");
    }
}

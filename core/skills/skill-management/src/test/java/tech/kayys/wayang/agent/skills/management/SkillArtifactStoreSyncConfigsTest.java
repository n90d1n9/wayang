package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillArtifactStoreSyncConfigsTest {

    @Test
    void parsesMirrorModeFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.artifacts.sync.mode", "mirror");

        SkillArtifactStoreSyncOptions options = SkillArtifactStoreSyncConfigs.fromProperties(properties);

        assertThat(options.overwriteExisting()).isTrue();
        assertThat(options.deleteMissingFromTarget()).isTrue();
        assertThat(options.dryRun()).isFalse();
    }

    @Test
    void parsesDryRunModeFromEnvironment() {
        SkillArtifactStoreSyncOptions options = SkillArtifactStoreSyncConfigs.fromEnvironment(Map.of(
                "WAYANG_SKILLS_ARTIFACTS_SYNC_MODE", "preview"));

        assertThat(options.overwriteExisting()).isTrue();
        assertThat(options.deleteMissingFromTarget()).isTrue();
        assertThat(options.dryRun()).isTrue();
    }

    @Test
    void parsesNestedMapOverrides() {
        SkillArtifactStoreSyncOptions options = SkillArtifactStoreSyncConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "skills", Map.of(
                                "artifacts", Map.of(
                                        "sync", Map.of(
                                                "mode", "mirror",
                                                "delete-missing", "false",
                                                "dry-run", "true"))))));

        assertThat(options.overwriteExisting()).isTrue();
        assertThat(options.deleteMissingFromTarget()).isFalse();
        assertThat(options.dryRun()).isTrue();
    }

    @Test
    void acceptsArtifactSpecificAliasProfile() {
        SkillArtifactStoreSyncOptions options = SkillArtifactStoreSyncConfigs.fromMap(Map.of(
                "wayang.skills.artifacts.sync.replace-existing", "true",
                "wayang.skills.artifacts.sync.prune", "true"));

        assertThat(options.overwriteExisting()).isTrue();
        assertThat(options.deleteMissingFromTarget()).isTrue();
        assertThat(options.dryRun()).isFalse();
    }

    @Test
    void defaultsToBootstrap() {
        SkillArtifactStoreSyncOptions options = SkillArtifactStoreSyncConfigs.fromMap(Map.of());

        assertThat(options).isEqualTo(SkillArtifactStoreSyncOptions.bootstrap());
    }

    @Test
    void usesProvidedDefaultsWhenModeIsAbsent() {
        SkillArtifactStoreSyncOptions options = SkillArtifactStoreSyncConfigs.fromNormalizedMap(
                SkillStoreConfigValues.flattenAndNormalize(Map.of(
                        "maintenance.artifacts.sync.delete-missing", "true",
                        "maintenance.artifacts.sync.dry-run", "true")),
                "maintenance.artifacts.sync.",
                SkillArtifactStoreSyncOptions.bootstrap());

        assertThat(options.overwriteExisting()).isFalse();
        assertThat(options.deleteMissingFromTarget()).isTrue();
        assertThat(options.dryRun()).isTrue();
    }

    @Test
    void rejectsUnknownMode() {
        assertThatThrownBy(() -> SkillArtifactStoreSyncConfigs.fromMap(Map.of(
                "wayang.skills.artifacts.sync.mode", "mystery")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown artifact sync mode");
    }
}

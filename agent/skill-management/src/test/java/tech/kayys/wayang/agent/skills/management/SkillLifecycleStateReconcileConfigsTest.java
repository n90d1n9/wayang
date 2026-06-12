package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillLifecycleStateReconcileConfigsTest {

    @Test
    void parsesCreateMissingModeFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.lifecycle.reconcile.mode", "bootstrap");

        SkillLifecycleStateReconcileOptions options =
                SkillLifecycleStateReconcileConfigs.fromProperties(properties);

        assertThat(options.createMissingStates()).isTrue();
        assertThat(options.removeOrphanedStates()).isFalse();
    }

    @Test
    void parsesSyncModeFromEnvironment() {
        SkillLifecycleStateReconcileOptions options = SkillLifecycleStateReconcileConfigs.fromEnvironment(Map.of(
                "WAYANG_SKILLS_LIFECYCLE_RECONCILE_MODE", "sync"));

        assertThat(options.createMissingStates()).isTrue();
        assertThat(options.removeOrphanedStates()).isTrue();
    }

    @Test
    void booleanOverridesCanTuneModeFromNestedMap() {
        SkillLifecycleStateReconcileOptions options = SkillLifecycleStateReconcileConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "skills", Map.of(
                                "lifecycle", Map.of(
                                        "reconcile", Map.of(
                                                "mode", "sync",
                                                "remove-orphans", "false"))))));

        assertThat(options.createMissingStates()).isTrue();
        assertThat(options.removeOrphanedStates()).isFalse();
    }

    @Test
    void defaultsToInspectOnly() {
        SkillLifecycleStateReconcileOptions options = SkillLifecycleStateReconcileConfigs.fromMap(Map.of());

        assertThat(options.createMissingStates()).isFalse();
        assertThat(options.removeOrphanedStates()).isFalse();
    }

    @Test
    void normalizedMapCanApplyOverridesToCallerDefaults() {
        SkillLifecycleStateReconcileOptions options = SkillLifecycleStateReconcileConfigs.fromNormalizedMap(
                SkillStoreConfigValues.flattenAndNormalize(Map.of(
                        "maintenance.lifecycle.reconcile.remove-orphans", "false")),
                "maintenance.lifecycle.reconcile.",
                SkillLifecycleStateReconcileOptions.createMissingAndRemoveOrphans());

        assertThat(options.createMissingStates()).isTrue();
        assertThat(options.removeOrphanedStates()).isFalse();
    }

    @Test
    void normalizedMapModeOverridesCallerDefaults() {
        SkillLifecycleStateReconcileOptions options = SkillLifecycleStateReconcileConfigs.fromNormalizedMap(
                SkillStoreConfigValues.flattenAndNormalize(Map.of(
                        "maintenance.lifecycle.reconcile.mode", "inspect")),
                "maintenance.lifecycle.reconcile.",
                SkillLifecycleStateReconcileOptions.createMissingAndRemoveOrphans());

        assertThat(options).isEqualTo(SkillLifecycleStateReconcileOptions.inspectOnly());
    }

    @Test
    void rejectsUnknownMode() {
        assertThatThrownBy(() -> SkillLifecycleStateReconcileConfigs.fromMap(Map.of(
                "wayang.skills.lifecycle.reconcile.mode", "mystery")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown lifecycle reconcile mode");
    }
}

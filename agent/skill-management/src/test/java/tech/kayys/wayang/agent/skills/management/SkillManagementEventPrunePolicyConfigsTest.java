package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementEventPrunePolicyConfigsTest {

    @Test
    void defaultsToDisabledPolicy() {
        SkillManagementEventPrunePolicy policy = SkillManagementEventPrunePolicyConfigs.fromMap(Map.of());

        assertThat(policy.enabled()).isFalse();
        assertThat(policy.options().keepLatestEvents())
                .isEqualTo(InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS);
    }

    @Test
    void parsesPreviewModeFromEnvironment() {
        SkillManagementEventPrunePolicy policy = SkillManagementEventPrunePolicyConfigs.fromEnvironment(Map.of(
                "WAYANG_SKILLS_EVENTS_PRUNE_MODE", "preview",
                "WAYANG_SKILLS_EVENTS_PRUNE_KEEP_LATEST_EVENTS", "7"));

        assertThat(policy.enabled()).isTrue();
        assertThat(policy.options().keepLatestEvents()).isEqualTo(7);
        assertThat(policy.options().dryRun()).isTrue();
    }

    @Test
    void parsesEnabledApplyPolicyFromProperties() {
        Properties properties = new Properties();
        properties.setProperty("wayang.skills.events.prune.enabled", "true");
        properties.setProperty("wayang.skills.events.prune.keep-latest", "12");
        properties.setProperty("wayang.skills.events.prune.dry-run", "false");

        SkillManagementEventPrunePolicy policy = SkillManagementEventPrunePolicyConfigs.fromProperties(properties);

        assertThat(policy.enabled()).isTrue();
        assertThat(policy.options().keepLatestEvents()).isEqualTo(12);
        assertThat(policy.options().dryRun()).isFalse();
    }

    @Test
    void rejectsUnknownMode() {
        assertThatThrownBy(() -> SkillManagementEventPrunePolicyConfigs.fromMap(Map.of(
                "wayang.skills.events.prune.mode", "mystery")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown skill-management event prune mode");
    }
}

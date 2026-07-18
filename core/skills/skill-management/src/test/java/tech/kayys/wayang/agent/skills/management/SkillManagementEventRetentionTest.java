package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementEventRetentionTest {

    @Test
    void resolvesNullOptionsToDefaultRetention() {
        SkillManagementEventPruneOptions resolved = SkillManagementEventRetention.resolve(null, 3);

        assertThat(resolved.keepLatestEvents()).isEqualTo(3);
        assertThat(resolved.dryRun()).isFalse();
    }

    @Test
    void normalizesNegativeRetentionOptions() {
        assertThat(SkillManagementEventPruneOptions.keepLatest(-3).keepLatestEvents()).isZero();
        assertThat(SkillManagementEventPruneOptions.dryRun(-4).keepLatestEvents()).isZero();
    }

    @Test
    void normalizesStorageCapacity() {
        assertThat(SkillManagementEventRetention.normalizeCapacity(0)).isEqualTo(1);
        assertThat(SkillManagementEventRetention.normalizeCapacity(-5)).isEqualTo(1);
        assertThat(SkillManagementEventRetention.normalizeCapacity(25)).isEqualTo(25);
    }

    @Test
    void normalizesConfiguredStorageCapacityWithDefault() {
        assertThat(SkillManagementEventRetention.normalizeCapacityOrDefault(
                0,
                InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS))
                .isEqualTo(InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS);
        assertThat(SkillManagementEventRetention.normalizeCapacityOrDefault(-5, 25)).isEqualTo(25);
        assertThat(SkillManagementEventRetention.normalizeCapacityOrDefault(5, 25)).isEqualTo(5);
    }

    @Test
    void calculatesRemovableCounts() {
        assertThat(SkillManagementEventRetention.removableCount(5, 2)).isEqualTo(3);
        assertThat(SkillManagementEventRetention.removableCount(2, 5)).isZero();
        assertThat(SkillManagementEventRetention.removableCount(-1, 0)).isZero();
    }

    @Test
    void selectsOldestEventsToPrune() {
        assertThat(SkillManagementEventRetention.oldestToPrune(List.of("a", "b", "c", "d"), 2))
                .containsExactly("a", "b");
        assertThat(SkillManagementEventRetention.oldestToPrune(List.of("a", "b"), 5))
                .isEmpty();
        assertThat(SkillManagementEventRetention.oldestToPrune(null, 1))
                .isEmpty();
    }
}

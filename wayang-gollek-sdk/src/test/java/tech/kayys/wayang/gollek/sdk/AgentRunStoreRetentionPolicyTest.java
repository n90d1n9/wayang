package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunStoreRetentionPolicyTest {

    @Test
    void parsesNestedRetentionPolicyMaps() {
        AgentRunStoreRetentionPolicy policy = AgentRunStoreRetentionPolicy.fromMap(Map.of(
                "retention", Map.of(
                        "maxRuns", "7",
                        "maxEventsPerRun", 3)));

        assertThat(policy).isEqualTo(AgentRunStoreRetentionPolicy.of(7, 3));
        assertThat(policy.bounded()).isTrue();
        assertThat(policy.isUnlimited()).isFalse();
    }

    @Test
    void parsesRootLevelRetentionAliases() {
        AgentRunStoreRetentionPolicy policy = AgentRunStoreRetentionPolicy.fromMap(Map.of(
                "runLimit", 9,
                "timelineLimit", "4"));

        assertThat(policy.maxRuns()).isEqualTo(9);
        assertThat(policy.maxEventsPerRun()).isEqualTo(4);
    }

    @Test
    void parsesExplicitUnlimitedRetentionMode() {
        AgentRunStoreRetentionPolicy nested = AgentRunStoreRetentionPolicy.fromMap(Map.of(
                "retention", Map.of(
                        "mode", "unlimited",
                        "maxRuns", 7,
                        "maxEventsPerRun", 3)));
        AgentRunStoreRetentionPolicy scalar = AgentRunStoreRetentionPolicy.fromMap(Map.of(
                "retention", "off"));
        AgentRunStoreRetentionPolicy root = AgentRunStoreRetentionPolicy.fromMap(Map.of(
                "mode", "unbounded",
                "maxRuns", 7,
                "maxEventsPerRun", 3));

        assertThat(nested).isEqualTo(AgentRunStoreRetentionPolicy.unlimited());
        assertThat(scalar).isEqualTo(AgentRunStoreRetentionPolicy.unlimited());
        assertThat(root).isEqualTo(AgentRunStoreRetentionPolicy.unlimited());
    }

    @Test
    void parsesBooleanUnlimitedRetentionAliases() {
        assertThat(AgentRunStoreRetentionPolicy.fromMap(Map.of(
                "retention", Map.of("unlimited", true))))
                .isEqualTo(AgentRunStoreRetentionPolicy.unlimited());
        assertThat(AgentRunStoreRetentionPolicy.fromMap(Map.of(
                "retention", Map.of("enabled", false))))
                .isEqualTo(AgentRunStoreRetentionPolicy.unlimited());
        assertThat(AgentRunStoreRetentionPolicy.fromMap(Map.of(
                "retention", false)))
                .isEqualTo(AgentRunStoreRetentionPolicy.unlimited());
    }

    @Test
    void fallsBackForMalformedRetentionValues() {
        AgentRunStoreRetentionPolicy policy = AgentRunStoreRetentionPolicy.fromMap(Map.of(
                "retention", Map.of(
                        "maxRuns", "many",
                        "eventLimit", "few")));
        AgentRunStoreRetentionPolicy booleanPolicy = AgentRunStoreRetentionPolicy.fromMap(Map.of(
                "retention", Map.of("enabled", "maybe")));

        assertThat(policy).isEqualTo(AgentRunStoreRetentionPolicy.defaults());
        assertThat(booleanPolicy).isEqualTo(AgentRunStoreRetentionPolicy.defaults());
    }

    @Test
    void clampsNegativeValuesAndReportsUnlimitedPolicy() {
        AgentRunStoreRetentionPolicy policy = AgentRunStoreRetentionPolicy.of(-1, -2);

        assertThat(policy.maxRuns()).isZero();
        assertThat(policy.maxEventsPerRun()).isZero();
        assertThat(policy.mode()).isEqualTo("unlimited");
        assertThat(policy.bounded()).isFalse();
        assertThat(policy.isUnlimited()).isTrue();
    }

    @Test
    void serializesPolicyToStableMapShape() {
        Map<String, Object> values = AgentRunStoreRetentionPolicy.of(7, 3).toMap();

        assertThat(values)
                .containsEntry("mode", "bounded")
                .containsEntry("maxRuns", 7)
                .containsEntry("maxEventsPerRun", 3)
                .containsEntry("runsBounded", true)
                .containsEntry("eventsPerRunBounded", true)
                .containsEntry("bounded", true)
                .containsEntry("unlimited", false);
        assertThat(AgentRunStoreRetentionPolicy.toMap(null))
                .containsEntry("mode", "bounded")
                .containsEntry("maxRuns", AgentRunStoreRetentionPolicy.DEFAULT_MAX_RUNS)
                .containsEntry("maxEventsPerRun", AgentRunStoreRetentionPolicy.DEFAULT_MAX_EVENTS_PER_RUN)
                .containsEntry("bounded", true)
                .containsEntry("unlimited", false);
        assertThat(AgentRunStoreRetentionPolicy.unlimited().toMap())
                .containsEntry("mode", "unlimited")
                .containsEntry("maxRuns", 0)
                .containsEntry("maxEventsPerRun", 0)
                .containsEntry("bounded", false)
                .containsEntry("unlimited", true);
    }
}

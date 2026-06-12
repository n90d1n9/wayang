package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillStoreConfigKeysTest {

    @Test
    void resolvesSharedStoreConfigAliases() {
        SkillStoreConfigValues.ScopedValues scoped = scoped(Map.of(
                "wayang.skills.store.backend", "jdbc",
                "wayang.skills.store.strategy", "memory",
                "wayang.skills.store.filesystem.directory", "/tmp/skills",
                "wayang.skills.store.object-prefix", "tenant-a/skills",
                "wayang.skills.store.jdbc.table", "skill_defs",
                "wayang.skills.store.jdbc.initialize-schema", "false",
                "wayang.skills.store.custom.name", "tenant-store",
                "wayang.skills.store.primary.kind", "memory",
                "wayang.skills.store.fallback.kind", "filesystem"));

        assertThat(SkillStoreConfigKeys.storeKind(scoped, "registry")).isEqualTo("jdbc");
        assertThat(SkillStoreConfigKeys.mode(scoped, "bootstrap")).isEqualTo("memory");
        assertThat(SkillStoreConfigKeys.hasMode(scoped)).isTrue();
        assertThat(SkillStoreConfigKeys.directory(scoped, "missing")).isEqualTo("/tmp/skills");
        assertThat(SkillStoreConfigKeys.objectPrefix(scoped, "default")).isEqualTo("tenant-a/skills");
        assertThat(SkillStoreConfigKeys.jdbcTableName(scoped, "default_table")).isEqualTo("skill_defs");
        assertThat(SkillStoreConfigKeys.initializeJdbcSchema(scoped)).isFalse();
        assertThat(SkillStoreConfigKeys.customStoreName(scoped, "missing")).isEqualTo("tenant-store");
    }

    @Test
    void appliesDefaultsForOptionalAliases() {
        SkillStoreConfigValues.ScopedValues scoped = scoped(Map.of());

        assertThat(SkillStoreConfigKeys.storeKind(scoped, "memory")).isEqualTo("memory");
        assertThat(SkillStoreConfigKeys.mode(scoped, "bootstrap")).isEqualTo("bootstrap");
        assertThat(SkillStoreConfigKeys.hasMode(scoped)).isFalse();
        assertThat(SkillStoreConfigKeys.dryRun(scoped, true)).isTrue();
        assertThat(SkillStoreConfigKeys.objectPrefix(scoped, "tenant-a/default")).isEqualTo("tenant-a/default");
        assertThat(SkillStoreConfigKeys.jdbcTableName(scoped, "skill_table")).isEqualTo("skill_table");
        assertThat(SkillStoreConfigKeys.initializeJdbcSchema(scoped)).isTrue();
        assertThat(SkillStoreConfigKeys.eventMaxEvents(scoped, 25, "invalid max")).isEqualTo(25);
        assertThat(SkillStoreConfigKeys.keepLatestEvents(scoped, 10, "invalid retention")).isEqualTo(10);
    }

    @Test
    void resolvesSharedMaintenanceAliases() {
        assertThat(SkillStoreConfigKeys.mode(
                scoped(Map.of("wayang.skills.store.strategy", "repair")),
                "bootstrap"))
                .isEqualTo("repair");
        assertThat(SkillStoreConfigKeys.mode(
                scoped(Map.of("wayang.skills.store.policy", "inspect")),
                "bootstrap"))
                .isEqualTo("inspect");
        assertThat(SkillStoreConfigKeys.dryRun(
                scoped(Map.of("wayang.skills.store.plan-only", "true")),
                false))
                .isTrue();
        assertThat(SkillStoreConfigKeys.dryRun(
                scoped(Map.of("wayang.skills.store.preview", "false")),
                true))
                .isFalse();
    }

    @Test
    void resolvesEventHistoryLimitAliases() {
        assertThat(SkillStoreConfigKeys.eventMaxEvents(
                scoped(Map.of("wayang.skills.store.max-events", "20")),
                25,
                "invalid max"))
                .isEqualTo(20);
        assertThat(SkillStoreConfigKeys.eventMaxEvents(
                scoped(Map.of("wayang.skills.store.retention", "30")),
                25,
                "invalid max"))
                .isEqualTo(30);
        assertThat(SkillStoreConfigKeys.keepLatestEvents(
                scoped(Map.of("wayang.skills.store.keep-latest-events", "7")),
                10,
                "invalid retention"))
                .isEqualTo(7);
        assertThat(SkillStoreConfigKeys.keepLatestEvents(
                scoped(Map.of("wayang.skills.store.limit", "8")),
                10,
                "invalid retention"))
                .isEqualTo(8);
    }

    @Test
    void reportsInvalidEventHistoryLimitsWithCallerMessage() {
        assertThatThrownBy(() -> SkillStoreConfigKeys.eventMaxEvents(
                scoped(Map.of("wayang.skills.store.max-events", "many")),
                25,
                "invalid max"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid max: many");
        assertThatThrownBy(() -> SkillStoreConfigKeys.keepLatestEvents(
                scoped(Map.of("wayang.skills.store.keep-latest", "several")),
                10,
                "invalid retention"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid retention: several");
    }

    @Test
    void validatesHybridChildGroups() {
        SkillStoreConfigValues.ScopedValues complete = scoped(Map.of(
                "wayang.skills.store.primary.kind", "memory",
                "wayang.skills.store.fallback.kind", "filesystem"));

        SkillStoreConfigKeys.requirePrimaryFallback(complete, "missing children");

        SkillStoreConfigValues.ScopedValues missingFallback = scoped(Map.of(
                "wayang.skills.store.primary.kind", "memory"));
        assertThatThrownBy(() -> SkillStoreConfigKeys.requirePrimaryFallback(missingFallback, "missing children"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("missing children");
    }

    @Test
    void buildsChildPrefixesFromNormalizedParentPrefix() {
        assertThat(SkillStoreConfigKeys.childPrefix("wayang.skills.store", "primary"))
                .isEqualTo("wayang.skills.store.primary.");
        assertThat(SkillStoreConfigKeys.childPrefix("", "fallback"))
                .isEqualTo("fallback.");
    }

    private SkillStoreConfigValues.ScopedValues scoped(Map<String, ?> values) {
        return new SkillStoreConfigValues.ScopedValues(
                SkillStoreConfigValues.flattenAndNormalize(values),
                "wayang.skills.store.");
    }
}

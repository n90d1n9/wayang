package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminValueSupportTest {

    @Test
    void centralizesSharedNormalizationRules() {
        List<SkillManagementAdminSyncChange> changes = java.util.Arrays.asList(
                new SkillManagementAdminSyncChange("planner", "COPIED", false, ""),
                null,
                new SkillManagementAdminSyncChange("writer", "UNCHANGED", true, ""));

        assertThat(SkillManagementAdminValueSupport.compactStrings(
                java.util.Arrays.asList("alpha", "", null, "beta")))
                .containsExactly("alpha", "beta");
        assertThat(SkillManagementAdminValueSupport.sortedDistinctStrings(
                java.util.Arrays.asList("writer", null, "planner", "writer")))
                .containsExactly("planner", "writer");
        assertThat(SkillManagementAdminValueSupport.text(null)).isBlank();
        assertThat(SkillManagementAdminValueSupport.identifier(" deploy-1 ")).isEqualTo("deploy-1");
        assertThat(SkillManagementAdminValueSupport.identifier(" ")).isBlank();
        assertThat(SkillManagementAdminValueSupport.blankToEmpty(" ")).isBlank();
        assertThat(SkillManagementAdminValueSupport.unknownIfBlank(" ")).isEqualTo("UNKNOWN");
        assertThat(SkillManagementAdminValueSupport.changedForSyncAction("DELETED", false)).isTrue();
        assertThat(SkillManagementAdminValueSupport.changedForSyncAction("CONFLICT", true)).isFalse();
        assertThat(SkillManagementAdminValueSupport.nonNegative(-4)).isZero();
        assertThat(SkillManagementAdminValueSupport.atLeast(-4, 3)).isEqualTo(3);
        assertThat(SkillManagementAdminValueSupport.nonNegativeCounts(Map.of(
                "disabled",
                -2,
                "active",
                1)))
                .containsEntry("active", 1)
                .containsEntry("disabled", 0);
        assertThat(SkillManagementAdminValueSupport.booleanAttribute(Map.of("dryRun", "true"), "dryRun"))
                .isTrue();
        assertThat(SkillManagementAdminValueSupport.nonNegativeIntAttribute(
                Map.of("preflightErrors", "-2"),
                "preflightErrors"))
                .isZero();
        assertThat(SkillManagementAdminValueSupport.nonNegativeIntAttribute(
                Map.of("preflightErrors", "oops"),
                "preflightErrors"))
                .isZero();
        assertThat(SkillManagementAdminValueSupport.hasAttributePrefix(
                Map.of("preflightMessage", "checked"),
                "preflight"))
                .isTrue();
        assertThat(SkillManagementAdminValueSupport.countAction(
                changes,
                SkillManagementAdminSyncChange::action,
                "COPIED")).isEqualTo(1);
        assertThat(SkillManagementAdminValueSupport.countMatching(
                changes,
                SkillManagementAdminSyncChange::changed)).isEqualTo(1);
    }
}

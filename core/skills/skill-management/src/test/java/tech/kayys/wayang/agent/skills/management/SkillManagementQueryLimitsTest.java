package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementQueryLimitsTest {

    @Test
    void normalizesAllBoundedQueryLimitsThroughOnePolicy() {
        assertThat(SkillManagementQueryLimits.normalize(0))
                .isEqualTo(SkillManagementQueryLimits.DEFAULT_LIMIT);
        assertThat(SkillManagementQueryLimits.normalize(-1))
                .isEqualTo(SkillManagementQueryLimits.DEFAULT_LIMIT);
        assertThat(SkillManagementQueryLimits.normalize(42)).isEqualTo(42);
        assertThat(SkillManagementQueryLimits.normalize(SkillManagementQueryLimits.MAX_LIMIT + 1))
                .isEqualTo(SkillManagementQueryLimits.MAX_LIMIT);
    }

    @Test
    void publicQueryConstantsStayAlignedWithSharedPolicy() {
        assertThat(SkillManagementEventQuery.DEFAULT_LIMIT)
                .isEqualTo(SkillManagementQueryLimits.DEFAULT_LIMIT);
        assertThat(SkillManagementEventQuery.MAX_LIMIT)
                .isEqualTo(SkillManagementQueryLimits.MAX_LIMIT);
        assertThat(SkillArtifactQuery.DEFAULT_LIMIT)
                .isEqualTo(SkillManagementQueryLimits.DEFAULT_LIMIT);
        assertThat(SkillArtifactQuery.MAX_LIMIT)
                .isEqualTo(SkillManagementQueryLimits.MAX_LIMIT);
    }
}

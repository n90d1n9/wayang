package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementOperationTraceStatusTest {

    @Test
    void derivesTraceStatusFromNormalizedOperationIdRootAvailabilityAndFailureState() {
        assertThat(SkillManagementOperationTraceStatus.from(" ", true, false))
                .isEqualTo(SkillManagementOperationTraceStatus.MISSING_OPERATION_ID);
        assertThat(SkillManagementOperationTraceStatus.from("deployment-1", false, false))
                .isEqualTo(SkillManagementOperationTraceStatus.ROOT_MISSING);
        assertThat(SkillManagementOperationTraceStatus.from("deployment-1", true, true))
                .isEqualTo(SkillManagementOperationTraceStatus.FAILED);
        assertThat(SkillManagementOperationTraceStatus.from(" deployment-1 ", true, false))
                .isEqualTo(SkillManagementOperationTraceStatus.HEALTHY);
    }

    @Test
    void parsesApiFriendlyStatusLabels() {
        assertThat(SkillManagementOperationTraceStatus.parse(" failed "))
                .contains(SkillManagementOperationTraceStatus.FAILED);
        assertThat(SkillManagementOperationTraceStatus.parse("root-missing"))
                .contains(SkillManagementOperationTraceStatus.ROOT_MISSING);
        assertThat(SkillManagementOperationTraceStatus.parse("missing operation id"))
                .contains(SkillManagementOperationTraceStatus.MISSING_OPERATION_ID);
        assertThat(SkillManagementOperationTraceStatus.parse("")).isEmpty();
        assertThat(SkillManagementOperationTraceStatus.parse("unknown")).isEmpty();
    }
}

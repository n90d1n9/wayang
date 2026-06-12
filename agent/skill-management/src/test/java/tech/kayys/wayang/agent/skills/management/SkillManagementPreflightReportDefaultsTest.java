package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementPreflightReportDefaultsTest {

    @Test
    void emptyNeutralReportIsReadyWithValidBuckets() {
        SkillManagementPreflightReport report = SkillManagementPreflightReport.empty();

        assertThat(report.ready()).isTrue();
        assertThat(report.errors()).isEmpty();
        assertThat(report.configurationValidation().validConfiguration()).isTrue();
        assertThat(report.targetStoreValidation().validConfiguration()).isTrue();
        assertThat(report.sourceStoreValidation().validConfiguration()).isTrue();
        assertThat(report.capabilityValidation().validConfiguration()).isTrue();
    }

    @Test
    void neutralOrEmptyPreservesExistingReport() {
        SkillManagementPreflightReport report = new SkillManagementPreflightReport(
                null,
                SkillStoreConfigValidationResult.error("target failed"),
                null,
                null);

        assertThat(SkillManagementPreflightReport.orEmpty(report)).isSameAs(report);
        assertThat(SkillManagementPreflightReport.orEmpty(null).ready()).isTrue();
    }

    @Test
    void emptyDeploymentReportUsesDefaultConfigAndEmptyValidation() {
        SkillManagementDeploymentPreflightReport report = SkillManagementDeploymentPreflightReport.empty();

        assertThat(report.config()).isEqualTo(SkillManagementDeploymentConfig.defaults());
        assertThat(report.validation()).isEqualTo(SkillManagementPreflightReport.empty());
        assertThat(report.ready()).isTrue();
        assertThat(SkillManagementDeploymentPreflightReport.orEmpty(report)).isSameAs(report);
        assertThat(SkillManagementDeploymentPreflightReport.orEmpty(null)).isEqualTo(report);
    }
}

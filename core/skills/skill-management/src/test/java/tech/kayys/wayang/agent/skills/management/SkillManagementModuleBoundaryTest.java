package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementModuleBoundaryTest {

    @Test
    void namesTargetPackageSkeleton() {
        assertThat(SkillManagementModuleBoundary.basePackage())
                .isEqualTo("tech.kayys.wayang.agent.skills.management");
        assertThat(SkillManagementModuleBoundary.targetSubpackages())
                .extracting(SkillManagementModuleBoundary::packageName)
                .containsExactly(
                        "tech.kayys.wayang.agent.skills.management.config",
                        "tech.kayys.wayang.agent.skills.management.contracts",
                        "tech.kayys.wayang.agent.skills.management.preflight",
                        "tech.kayys.wayang.agent.skills.management.runtime",
                        "tech.kayys.wayang.agent.skills.management.store",
                        "tech.kayys.wayang.agent.skills.management.workflow",
                        "tech.kayys.wayang.agent.skills.management.events",
                        "tech.kayys.wayang.agent.skills.management.admin",
                        "tech.kayys.wayang.agent.skills.management.support");
    }

    @Test
    void exposesUniqueBoundaryLabelsAndResponsibilities() {
        assertThat(SkillManagementModuleBoundary.values())
                .extracting(SkillManagementModuleBoundary::label)
                .doesNotHaveDuplicates();
        assertThat(SkillManagementModuleBoundary.values())
                .allSatisfy(boundary -> assertThat(boundary.responsibility()).isNotBlank());
        assertThat(SkillManagementModuleBoundary.FACADE.rootPackage()).isTrue();
        assertThat(SkillManagementModuleBoundary.CONFIG.targetSubpackage()).isTrue();
    }
}

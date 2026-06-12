package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangBootstrapReportTest {

    @Test
    void bootstrapInstallsSkillsAndReturnsReadyReport() {
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory();
        AgenticCommerceTestSkillRegistry registry = new AgenticCommerceTestSkillRegistry();

        AgenticCommerceWayangBootstrapReport report = runtime.bootstrap(registry);
        Map<String, Object> values = report.toMap();

        assertThat(report.ready()).isTrue();
        assertThat(report.exitCode()).isZero();
        assertThat(report.bootstrapIssues()).isEmpty();
        assertThat(report.skillRegistration().definitionCount()).isEqualTo(5);
        assertThat(report.skillRegistration().runtimeSkillCount()).isEqualTo(5);
        assertThat(report.smokeProbe().passed()).isTrue();
        assertThat(report.bindingReport().routeCount()).isEqualTo(5);
        assertThat(registry.listSkills()).hasSize(5);
        assertThat(registry.listAll()).hasSize(5);
        assertThat(values)
                .containsEntry("ready", true)
                .containsEntry("exitCode", 0)
                .containsKeys("runtimeConfig", "bootstrapConfig", "skillRegistration", "smokeProbe", "bindingReport");
        assertThat(map(values.get("metadata"))).containsEntry("smokeEndpointEnabled", true);
        assertThat(map(values.get("bootstrapConfig"))).containsEntry("requireSkillRegistration", true);
    }

    @Test
    void bootstrapUsesDirectSmokeWhenPublicSmokeEndpointIsDisabled() {
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory(
                AgenticCommerceWayangRuntimeConfig.builder()
                        .httpConfig(AgenticCommerceHttpAdapterConfig.builder()
                                .smokeEnabled(false)
                                .build())
                        .build());
        AgenticCommerceTestSkillRegistry registry = new AgenticCommerceTestSkillRegistry();

        AgenticCommerceWayangBootstrapReport report = runtime.bootstrapDefinitions(registry);
        Map<String, Object> metadata = map(report.toMap().get("metadata"));

        assertThat(report.ready()).isTrue();
        assertThat(report.skillRegistration().definitionCount()).isEqualTo(5);
        assertThat(report.skillRegistration().runtimeSkillCount()).isZero();
        assertThat(report.smokeProbe().passed()).isTrue();
        assertThat(metadata).containsEntry("smokeEndpointEnabled", false);
        assertThat(registry.listSkills()).hasSize(5);
        assertThat(registry.listAll()).isEmpty();
    }

    @Test
    void bootstrapReportsMissingSelectedSkillIds() {
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory();
        AgenticCommerceTestSkillRegistry registry = new AgenticCommerceTestSkillRegistry();

        AgenticCommerceWayangBootstrapReport report = runtime.bootstrap(
                registry,
                List.of(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT, "missing-skill"),
                true,
                true);

        assertThat(report.ready()).isFalse();
        assertThat(report.failed()).isTrue();
        assertThat(report.exitCode()).isEqualTo(1);
        assertThat(report.bootstrapIssues()).containsExactly("skill_registration_incomplete");
        assertThat(report.skillRegistration().missingSkillIds()).containsExactly("missing-skill");
        assertThat(report.skillRegistration().definitionCount()).isEqualTo(1);
        assertThat(report.skillRegistration().runtimeSkillCount()).isEqualTo(1);
        assertThat(report.smokeProbe().passed()).isTrue();
    }

    @Test
    void bootstrapPolicyCanRelaxSkillRegistrationRequirement() {
        AgenticCommerceWayangRuntime runtime = AgenticCommerceWayangRuntime.inMemory();
        AgenticCommerceTestSkillRegistry registry = new AgenticCommerceTestSkillRegistry();

        AgenticCommerceWayangBootstrapReport report = runtime.bootstrap(
                registry,
                AgenticCommerceWayangBootstrapConfig.builder()
                        .skillIds(List.of(AgenticCommerceWayang.SKILL_CREATE_CHECKOUT, "missing-skill"))
                        .requireSkillRegistration(false)
                        .build());

        assertThat(report.ready()).isTrue();
        assertThat(report.exitCode()).isZero();
        assertThat(report.bootstrapIssues()).isEmpty();
        assertThat(report.skillRegistration().missingSkillIds()).containsExactly("missing-skill");
        assertThat(map(report.toMap().get("bootstrapConfig")))
                .containsEntry("requireSkillRegistration", false);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}

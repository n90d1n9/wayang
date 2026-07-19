package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsDefinitionQueryServiceTest {

    @Test
    void listsSkillsThroughRequest() {
        SkillManagementService managementService = managementService();
        managementService.createSkill(SkillsDefinitionRequest.fromOptions(
                "planner",
                "Planner",
                null,
                "REASONING",
                "Plan carefully.")
                .registrationDefinition()).await().indefinitely();
        SkillsDefinitionQueryService service = new SkillsDefinitionQueryService(managementService);

        SkillsDefinitionListReport report = service.list(
                SkillsDefinitionListRequest.fromOptions(" reasoning ", true));

        assertThat(report.empty()).isFalse();
        assertThat(report.skills())
                .extracting(skill -> skill.id())
                .containsExactly("planner");
    }

    @Test
    void resolvesSkillInfoWithLifecycleState() {
        SkillManagementService managementService = managementService();
        managementService.createSkill(SkillsDefinitionRequest.fromOptions(
                "planner",
                "Planner",
                null,
                "REASONING",
                "Plan carefully.")
                .registrationDefinition()).await().indefinitely();
        SkillsDefinitionQueryService service = new SkillsDefinitionQueryService(managementService);

        SkillsDefinitionInfoReport report = service.info(
                SkillsDefinitionInfoRequest.fromOptions("planner"))
                .orElseThrow();

        assertThat(report.skill().id()).isEqualTo("planner");
        assertThat(report.lifecycleState().skillId()).isEqualTo("planner");
        assertThat(report.lifecycleState().status().name()).isEqualTo("ACTIVE");
    }

    @Test
    void returnsEmptyInfoForMissingSkill() {
        SkillsDefinitionQueryService service = new SkillsDefinitionQueryService(managementService());

        assertThat(service.info(SkillsDefinitionInfoRequest.fromOptions("missing"))).isEmpty();
    }

    private SkillManagementService managementService() {
        return new SkillManagementService(new InMemoryCliSkillRegistry());
    }
}

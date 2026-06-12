package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsDefinitionInfoCommandServiceTest {

    @Test
    void resolvesFoundInfoCommandReport() {
        SkillManagementService managementService = managementService();
        createPlanner(managementService);
        SkillsDefinitionInfoCommandService service =
                new SkillsDefinitionInfoCommandService(new SkillsDefinitionQueryService(managementService));

        SkillsDefinitionInfoCommandReport report = service.report(SkillsDefinitionInfoRequest.fromOptions("planner"));

        assertThat(report.found()).isTrue();
        assertThat(report.skillId()).isEqualTo("planner");
        assertThat(report.info()).hasValueSatisfying(info ->
                assertThat(info.lifecycleState().status().name()).isEqualTo("ACTIVE"));
    }

    @Test
    void rendersFoundAndMissingInfoCommandReports() {
        SkillManagementService managementService = managementService();
        createPlanner(managementService);
        SkillsDefinitionInfoCommandService service =
                new SkillsDefinitionInfoCommandService(new SkillsDefinitionQueryService(managementService));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        SkillsDefinitionInfoCommandText.render(
                service.report(SkillsDefinitionInfoRequest.fromOptions("planner")),
                new PrintStream(out),
                new PrintStream(err));
        SkillsDefinitionInfoCommandText.render(
                service.report(SkillsDefinitionInfoRequest.fromOptions("missing")),
                new PrintStream(out),
                new PrintStream(err));

        assertThat(out.toString())
                .contains("id: planner")
                .contains("status: ACTIVE");
        assertThat(err.toString()).isEqualTo("Skill not found: missing\n");
    }

    private void createPlanner(SkillManagementService managementService) {
        managementService.createSkill(SkillsDefinitionRequest.fromOptions(
                "planner",
                "Planner",
                null,
                "REASONING",
                "Plan carefully.")
                .registrationDefinition()).await().indefinitely();
    }

    private SkillManagementService managementService() {
        return new SkillManagementService(new InMemoryCliSkillRegistry());
    }
}

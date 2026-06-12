package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsDefinitionCommandServiceTest {

    @Test
    void registersSkillThroughRequest() {
        SkillManagementService managementService = managementService();
        SkillsDefinitionCommandService service = new SkillsDefinitionCommandService(managementService);

        SkillsDefinitionRegistrationReport report = service.register(plannerRequest());

        assertThat(report.skill().id()).isEqualTo("planner");
        assertThat(managementService.getSkill("planner").await().indefinitely())
                .map(SkillDefinition::name)
                .contains("Planner");
    }

    @Test
    void validatesSkillThroughRequest() {
        SkillsDefinitionCommandService service = new SkillsDefinitionCommandService(managementService());

        SkillsDefinitionValidationReport report = service.validate(plannerRequest());

        assertThat(report.valid()).isTrue();
        assertThat(report.errors()).isEmpty();
    }

    @Test
    void reportsInvalidDefinitionErrors() {
        SkillsDefinitionCommandService service = new SkillsDefinitionCommandService(managementService());

        SkillsDefinitionValidationReport report = service.validate(SkillsDefinitionRequest.fromOptions(
                "bad",
                "",
                "",
                "custom",
                ""));

        assertThat(report.valid()).isFalse();
        assertThat(report.errors())
                .contains("Skill name is required")
                .contains("System prompt is required");
    }

    @Test
    void rendersDefinitionCommandText() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        SkillsDefinitionCommandService service = new SkillsDefinitionCommandService(managementService());

        SkillsDefinitionCommandText.renderRegistration(
                service.register(plannerRequest()),
                new PrintStream(out));
        SkillsDefinitionCommandText.renderValidation(
                service.validate(SkillsDefinitionRequest.fromOptions("bad", "", "", "custom", "")),
                new PrintStream(out),
                new PrintStream(err));

        assertThat(out.toString()).contains("Registered skill: planner");
        assertThat(err.toString())
                .contains("Skill name is required")
                .contains("System prompt is required");
    }

    private SkillsDefinitionRequest plannerRequest() {
        return SkillsDefinitionRequest.fromOptions(
                "planner",
                "Planner",
                null,
                "REASONING",
                "Plan carefully.");
    }

    private SkillManagementService managementService() {
        return new SkillManagementService(new InMemoryCliSkillRegistry());
    }
}

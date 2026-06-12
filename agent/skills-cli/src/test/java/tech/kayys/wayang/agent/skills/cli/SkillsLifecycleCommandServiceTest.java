package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsLifecycleCommandServiceTest {

    @Test
    void disablesSkillThroughRequest() {
        SkillsLifecycleCommandService service = new SkillsLifecycleCommandService(managementService());

        SkillsLifecycleCommandReport report = service.execute(SkillsLifecycleCommandRequest.disable("planner"));

        assertThat(report.action()).isEqualTo(SkillsLifecycleCommandRequest.Action.DISABLE);
        assertThat(report.state().skillId()).isEqualTo("planner");
        assertThat(report.state().status().name()).isEqualTo("DISABLED");
    }

    @Test
    void enablesSkillThroughRequest() {
        SkillManagementService managementService = managementService();
        managementService.disableSkill("planner").await().indefinitely();
        SkillsLifecycleCommandService service = new SkillsLifecycleCommandService(managementService);

        SkillsLifecycleCommandReport report = service.execute(SkillsLifecycleCommandRequest.enable("planner"));

        assertThat(report.action()).isEqualTo(SkillsLifecycleCommandRequest.Action.ENABLE);
        assertThat(report.state().skillId()).isEqualTo("planner");
        assertThat(report.state().status().name()).isEqualTo("ACTIVE");
    }

    @Test
    void rendersLifecycleCommandText() {
        SkillsLifecycleCommandReport report =
                new SkillsLifecycleCommandService(managementService())
                        .execute(SkillsLifecycleCommandRequest.disable("planner"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SkillsLifecycleCommandText.render(report, new PrintStream(out));

        assertThat(out.toString()).isEqualTo("Disabled skill: planner (DISABLED)\n");
    }

    private SkillManagementService managementService() {
        SkillManagementService managementService = new SkillManagementService(new InMemoryCliSkillRegistry());
        managementService.createSkill(SkillsDefinitionRequest.fromOptions(
                "planner",
                "Planner",
                null,
                "REASONING",
                "Plan carefully.")
                .registrationDefinition()).await().indefinitely();
        return managementService;
    }
}

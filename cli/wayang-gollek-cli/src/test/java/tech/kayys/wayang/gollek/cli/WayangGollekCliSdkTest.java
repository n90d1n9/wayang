package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.AgentRunRequest;
import tech.kayys.wayang.gollek.sdk.AgentRunResult;
import tech.kayys.wayang.gollek.sdk.AgentSkillQuery;
import tech.kayys.wayang.gollek.sdk.ProductSurface;
import tech.kayys.wayang.gollek.sdk.RegisteredSkill;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangPlatformStatus;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangGollekCliSdkTest {

    private final WayangGollekSdk service = WayangGollekSdk.local();

    @Test
    void describesWayangAsPlatformAboveGollekAndGamelan() {
        WayangPlatformStatus status = service.status();

        assertThat(status.productName()).isEqualTo("Wayang");
        assertThat(status.gollek().role()).contains("Inference");
        assertThat(status.gamelan().role()).contains("Workflow");
        assertThat(status.notes()).anySatisfy(note -> assertThat(note).contains("SDK"));
        assertThat(status.notes()).anySatisfy(note -> assertThat(note).contains("separate from Gollek CLI"));
        assertThat(status.notes()).anySatisfy(note -> assertThat(note).contains("agent workbench"));
        assertThat(status.activeSkills()).isEqualTo(12);
    }

    @Test
    void preparesAgentRunThroughWayangBoundaries() {
        AgentRunResult result = service.run(new AgentRunRequest(
                " plan next eval ",
                " tenant-a ",
                " model-a ",
                " workflow-a ",
                List.of("rag"),
                true,
                5));

        assertThat(result.successful()).isTrue();
        assertThat(result.answer()).contains("plan next eval");
        assertThat(result.metadata())
                .containsEntry("tenant", "tenant-a")
                .containsEntry("model", "model-a")
                .containsEntry("workflow", "workflow-a")
                .containsEntry("surface", "coding-agent")
                .containsEntry("memoryEnabled", true)
                .containsEntry("maxSteps", 5);
    }

    @Test
    void exposesProductSurfacesPoweredByCoreEngine() {
        assertThat(service.productSurfaces())
                .extracting(ProductSurface::id)
                .containsExactly("coding-agent", "assistant-agent", "workflow-platform", "platform-admin");
        assertThat(service.productSurfacePolicy("coding-agent").routingHints())
                .contains("inspect-workspace", "plan-harness");
        assertThat(service.assessRunPolicy(AgentRunRequest.builder()
                .prompt("plan")
                .surfaceId("workflow-platform")
                .build()).recommendations())
                .contains("Set a workflow id with --workflow <id>.");
    }

    @Test
    void buildsWorkbenchModelForPlainAndTambouiRenderers() {
        WayangWorkbenchModel model = service.workbench();

        assertThat(model.status().productName()).isEqualTo("Wayang");
        assertThat(model.productSurfaces())
                .extracting(ProductSurface::id)
                .contains("coding-agent", "workflow-platform");
        assertThat(model.commandPalette())
                .contains("workbench", "tui")
                .anySatisfy(command -> assertThat(command).contains("run <task>"));
        assertThat(model.commands())
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("run-workflow-skill");
                    assertThat(command.surfaceIds()).containsExactly("workflow-platform");
                });
        assertThat(model.nextActions())
                .anySatisfy(action -> assertThat(action).contains("Tamboui"));
    }

    @Test
    void exposesDefaultSkillRegistryForProductSurfaces() {
        assertThat(service.skills())
                .extracting(RegisteredSkill::id)
                .contains("repo.context", "rag.retrieve", "mcp.bridge", "workflow.gamelan");

        assertThat(service.skill("rag").id()).isEqualTo("rag.retrieve");

        assertThat(service.skills(new AgentSkillQuery(
                "assistant-agent",
                null,
                "rag",
                null,
                null,
                null,
                "query",
                "citations")))
                .singleElement()
                .satisfies(skill -> {
                    assertThat(skill.id()).isEqualTo("rag.retrieve");
                    assertThat(skill.descriptor().surfaceIds()).contains("assistant-agent");
                    assertThat(skill.descriptor().tags()).contains("rag", "docs");
                });
    }
}

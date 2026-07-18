package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tech.kayys.wayang.agent.planner.AgentRunPlanner;
import tech.kayys.wayang.agent.run.AgentRunPreparation;
import tech.kayys.wayang.agent.run.AgentRunPreview;
import tech.kayys.wayang.agent.run.AgentRunRequest;
import tech.kayys.wayang.agent.spi.AgentRequest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentRunPlannerTest {

    @Test
    void preparesCoreRequestPreviewAndReadinessFromOnePlan(@TempDir Path workspace) throws Exception {
        Files.writeString(workspace.resolve("pom.xml"), "<project />");
        AgentRunPlanner planner = new AgentRunPlanner();

        AgentRunPreparation preparation = planner.prepare(AgentRunRequest.builder()
                .prompt("make a safe change")
                .tenantId("tenant-a")
                .modelId("model-a")
                .workspace(workspace.toString())
                .harness(4, false)
                .skill("repo")
                .build());

        AgentRequest coreRequest = preparation.coreRequest();
        AgentRunPreview preview = preparation.preview();

        assertThat(preparation.request().surfaceId()).isEqualTo("coding-agent");
        assertThat(preparation.workspace()).isNotNull();
        assertThat(preparation.harness()).isNotNull();
        assertThat(preparation.ready()).isTrue();
        assertThat(preparation.readiness().surfacePolicyAssessment().ready()).isTrue();
        assertThat(preparation.readiness().skillAssessment().ready()).isTrue();
        assertThat(coreRequest.context()).containsKeys("surfacePolicy", "surfacePolicyAssessment", "workspace", "harness");
        assertThat(coreRequest.allowedSkills()).containsExactly("repo");
        assertThat(preview.ready()).isTrue();
        assertThat(preview.workspaceAttached()).isTrue();
        assertThat(preview.harnessAttached()).isTrue();
        assertThat(preview.context()).containsKeys("workspace", "harness");
    }

    @Test
    void appliesConfiguredTenantAndModelDefaults() {
        AgentRunPlanner planner = new AgentRunPlanner(new WayangGollekSdkConfig(
                WayangGollekSdkProvider.Mode.LOCAL,
                "",
                "",
                "tenant-default",
                "model-default",
                ""));

        AgentRunPreparation preparation = planner.prepare(AgentRunRequest.builder()
                .prompt("answer")
                .build());

        assertThat(preparation.request().tenantId()).isEqualTo("tenant-default");
        assertThat(preparation.request().modelId()).isEqualTo("model-default");
        assertThat(preparation.coreRequest().tenantId()).isEqualTo("tenant-default");
        assertThat(preparation.coreRequest().modelId()).isEqualTo("model-default");
    }

    @Test
    void rejectsUnknownSurfaceBeforePreparingCoreRequest() {
        AgentRunPlanner planner = new AgentRunPlanner();

        assertThatThrownBy(() -> planner.prepare(AgentRunRequest.builder()
                .prompt("answer")
                .surfaceId("future-agent")
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang product surface 'future-agent'");
    }
}

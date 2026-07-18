package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.run.AgentRunRequest;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.harness.HarnessCheck;
import tech.kayys.wayang.harness.HarnessPlan;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangAgentRequestMapperTest {

    @Test
    void mapsSdkRunRequestToCoreAgentRequest() {
        AgentRequest request = new WayangAgentRequestMapper().toAgentRequest(new AgentRunRequest(
                " plan ",
                " tenant-a ",
                " model-a ",
                " workflow-a ",
                List.of("rag", "mcp"),
                true,
                4,
                ".",
                false,
                80,
                false,
                8,
                true,
                "assistant-agent"));

        assertThat(request.prompt()).isEqualTo("plan");
        assertThat(request.tenantId()).isEqualTo("tenant-a");
        assertThat(request.modelId()).isEqualTo("model-a");
        assertThat(request.getMaxSteps()).isEqualTo(4);
        assertThat(request.allowedSkills()).containsExactly("rag", "mcp");
        assertThat(request.context()).containsEntry("workflowId", "workflow-a");
        assertThat(request.context()).containsEntry("surfaceId", "assistant-agent");
        assertThat(request.context()).containsKey("surfacePolicy");
        @SuppressWarnings("unchecked")
        Map<String, Object> surfacePolicy = (Map<String, Object>) request.context().get("surfacePolicy");
        assertThat(surfacePolicy)
                .containsEntry("surfaceId", "assistant-agent")
                .containsEntry("memoryPreferred", true)
                .containsEntry("workspacePreferred", false);
        assertThat((List<String>) surfacePolicy.get("routingHints")).contains("prefer-rag");
        assertThat(request.context()).containsKey("surfacePolicyAssessment");
        @SuppressWarnings("unchecked")
        Map<String, Object> surfaceAssessment = (Map<String, Object>) request.context().get("surfacePolicyAssessment");
        assertThat(surfaceAssessment)
                .containsEntry("surfaceId", "assistant-agent")
                .containsEntry("ready", true);
        assertThat((List<String>) surfaceAssessment.get("missingContextKeys")).isEmpty();
        assertThat(request.memoryConfig().conversationEnabled()).isTrue();
        assertThat(request.memoryConfig().vectorMemoryEnabled()).isTrue();
        assertThat(request.memoryConfig().memoryNamespace()).isEqualTo("tenant-a");
    }

    @Test
    void mapsWorkspaceSnapshotToCoreAgentContext() {
        WorkspaceSnapshot snapshot = new WorkspaceSnapshot(
                "/repo",
                true,
                true,
                true,
                "/repo",
                "main",
                List.of("pom.xml"),
                List.of("maven"),
                List.of("agent"),
                List.of("agent/", "pom.xml"),
                List.of("inspected"));

        AgentRequest request = new WayangAgentRequestMapper().toAgentRequest(
                new AgentRunRequest(
                        " plan ",
                        " tenant-a ",
                        " model-a ",
                        " workflow-a ",
                        List.of("rag"),
                        true,
                        4,
                        "/repo",
                        true,
                        20),
                snapshot);

        assertThat(request.context()).containsKey("workspace");
        @SuppressWarnings("unchecked")
        Map<String, Object> workspace = (Map<String, Object>) request.context().get("workspace");
        assertThat(workspace)
                .containsEntry("rootPath", "/repo")
                .containsEntry("gitRepository", true)
                .containsEntry("branch", "main");
        assertThat((List<String>) workspace.get("buildFiles")).contains("pom.xml");
        assertThat((List<String>) workspace.get("modules")).contains("agent");
    }

    @Test
    void mapsHarnessPlanToCoreAgentContext() {
        HarnessPlan harness = new HarnessPlan(
                new WorkspaceSnapshot(
                        "/repo",
                        true,
                        true,
                        true,
                        "/repo",
                        "main",
                        List.of("pom.xml"),
                        List.of("maven"),
                        List.of("agent"),
                        List.of("agent/", "pom.xml"),
                        List.of("inspected")),
                List.of(new HarnessCheck(
                        "maven-test",
                        "Maven test suite",
                        List.of("mvn", "-q", "test"),
                        "/repo",
                        false,
                        "Run tests.")),
                List.of("planned"));

        AgentRequest request = new WayangAgentRequestMapper().toAgentRequest(
                new AgentRunRequest(
                        " plan ",
                        " tenant-a ",
                        " model-a ",
                        "",
                        List.of("rag"),
                        true,
                        4,
                        "/repo",
                        false,
                        20,
                        true,
                        8,
                        true),
                null,
                harness);

        assertThat(request.context()).containsKey("harness");
        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) request.context().get("harness");
        assertThat(context).containsEntry("workspaceRoot", "/repo");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) context.get("checks");
        assertThat(checks).singleElement()
                .satisfies(check -> assertThat(check)
                        .containsEntry("id", "maven-test")
                        .containsEntry("commandLine", "mvn -q test")
                        .containsEntry("optional", false));
    }

    @Test
    void mapsRunIdentityAndAdditionalContextToCoreAgentRequest() {
        AgentRequest request = new WayangAgentRequestMapper().toAgentRequest(AgentRunRequest.builder()
                .prompt("answer")
                .systemPrompt("be concise")
                .tenantId("tenant-a")
                .surfaceId("assistant-agent")
                .sessionId("session-a")
                .userId("user-a")
                .context("rag.collection", "docs")
                .context("mcp.server", "filesystem")
                .context("surfaceId", "untrusted")
                .build());

        assertThat(request.sessionId()).isEqualTo("session-a");
        assertThat(request.userId()).isEqualTo("user-a");
        assertThat(request.systemPrompt()).isEqualTo("be concise");
        assertThat(request.context())
                .containsEntry("rag.collection", "docs")
                .containsEntry("mcp.server", "filesystem")
                .containsEntry("surfaceId", "assistant-agent");
    }

    @Test
    void rejectsUnknownProductSurfaceBeforeCoreMapping() {
        AgentRunRequest request = AgentRunRequest.builder()
                .prompt("plan")
                .surfaceId("future-agent")
                .build();

        assertThatThrownBy(() -> new WayangAgentRequestMapper().toAgentRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang product surface 'future-agent'")
                .hasMessageContaining("coding-agent")
                .hasMessageContaining("assistant-agent");
    }
}

package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.run.AgentRunRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunRequestTest {

    @Test
    void builderNormalizesRunOptionsByName() {
        AgentRunRequest request = AgentRunRequest.builder()
                .prompt(" plan ")
                .systemPrompt(" be concise ")
                .tenantId(" tenant-a ")
                .modelId(" model-a ")
                .workflowId(" workflow-a ")
                .surfaceId(" assistant-agent ")
                .sessionId(" session-a ")
                .userId(" user-a ")
                .context(" rag.collection ", " docs ")
                .context("", "ignored")
                .context("empty", null)
                .skill(" rag ")
                .skill("")
                .memoryEnabled(false)
                .maxSteps(0)
                .workspace(" /repo ", 0)
                .harness(0, false)
                .build();

        assertThat(request.prompt()).isEqualTo("plan");
        assertThat(request.systemPrompt()).isEqualTo("be concise");
        assertThat(request.tenantId()).isEqualTo("tenant-a");
        assertThat(request.modelId()).isEqualTo("model-a");
        assertThat(request.workflowId()).isEqualTo("workflow-a");
        assertThat(request.surfaceId()).isEqualTo("assistant-agent");
        assertThat(request.sessionId()).isEqualTo("session-a");
        assertThat(request.userId()).isEqualTo("user-a");
        assertThat(request.context()).containsExactly(Map.entry("rag.collection", "docs"));
        assertThat(request.skills()).containsExactly("rag");
        assertThat(request.memoryEnabled()).isFalse();
        assertThat(request.maxSteps()).isEqualTo(12);
        assertThat(request.workspaceEnabled()).isTrue();
        assertThat(request.workspacePath()).isEqualTo("/repo");
        assertThat(request.workspaceMaxEntries()).isEqualTo(80);
        assertThat(request.harnessEnabled()).isTrue();
        assertThat(request.harnessMaxChecks()).isEqualTo(8);
        assertThat(request.harnessIncludeOptional()).isFalse();
    }

    @Test
    void builderCanCopyAndOverrideExistingRequest() {
        AgentRunRequest source = AgentRunRequest.builder()
                .prompt("plan")
                .systemPrompt("be concise")
                .tenantId("tenant-a")
                .surfaceId("assistant-agent")
                .skills(List.of("rag"))
                .workspace("/repo")
                .harness(4, true)
                .build();

        AgentRunRequest copy = AgentRunRequest.builder(source)
                .tenantId("tenant-b")
                .skill("mcp")
                .build();

        assertThat(copy.prompt()).isEqualTo("plan");
        assertThat(copy.systemPrompt()).isEqualTo("be concise");
        assertThat(copy.tenantId()).isEqualTo("tenant-b");
        assertThat(copy.surfaceId()).isEqualTo("assistant-agent");
        assertThat(copy.skills()).containsExactly("rag", "mcp");
        assertThat(copy.workspaceEnabled()).isTrue();
        assertThat(copy.workspacePath()).isEqualTo("/repo");
        assertThat(copy.harnessEnabled()).isTrue();
        assertThat(copy.harnessMaxChecks()).isEqualTo(4);
    }

    @Test
    void defaultsBlankSurfaceToCodingAgent() {
        AgentRunRequest request = AgentRunRequest.builder()
                .prompt("plan")
                .surfaceId(" ")
                .build();

        assertThat(request.surfaceId()).isEqualTo("coding-agent");
    }
}

package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.AgentRunRequest;
import tech.kayys.wayang.gollek.sdk.Wayang;
import tech.kayys.wayang.gollek.sdk.WayangClient;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangRunRequestOptionsTest {

    @Test
    void requestOptionsOverlaySpecDefaults() {
        AgentRunRequest spec = AgentRunRequest.builder()
                .prompt("spec prompt")
                .systemPrompt("spec system")
                .tenantId("tenant-spec")
                .modelId("model-spec")
                .workflowId("workflow-spec")
                .surfaceId("coding-agent")
                .skills(List.of("repo"))
                .memoryEnabled(true)
                .maxSteps(9)
                .workspace("spec-workspace", 20)
                .harness(7, true)
                .context(Map.of("rag.collection", "spec-docs"))
                .build();

        WayangRunRequestOptions options = new WayangRunRequestOptions();
        options.prompt = "cli prompt";
        options.systemPrompt = "cli system";
        options.tenantId = "tenant-cli";
        options.modelId = "model-cli";
        options.sessionId = "session-cli";
        options.userId = "user-cli";
        options.workflowId = "workflow-cli";
        options.surfaceId = "assistant-agent";
        options.skills = List.of("rag", "mcp");
        options.contextEntries = List.of("mcp.server=filesystem");
        options.noMemory = true;
        options.maxSteps = 4;
        options.workspacePath = "cli-workspace";
        options.workspaceMaxEntries = 5;
        options.harnessEnabled = true;
        options.harnessMaxChecks = 3;
        options.harnessRequiredOnly = true;

        AgentRunRequest request = options.toRequest(spec, InputStream.nullInputStream());

        assertThat(request.prompt()).isEqualTo("cli prompt");
        assertThat(request.systemPrompt()).isEqualTo("cli system");
        assertThat(request.tenantId()).isEqualTo("tenant-cli");
        assertThat(request.modelId()).isEqualTo("model-cli");
        assertThat(request.sessionId()).isEqualTo("session-cli");
        assertThat(request.userId()).isEqualTo("user-cli");
        assertThat(request.workflowId()).isEqualTo("workflow-cli");
        assertThat(request.surfaceId()).isEqualTo("assistant-agent");
        assertThat(request.skills()).containsExactly("rag", "mcp");
        assertThat(request.context())
                .containsEntry("rag.collection", "spec-docs")
                .containsEntry("mcp.server", "filesystem");
        assertThat(request.memoryEnabled()).isFalse();
        assertThat(request.maxSteps()).isEqualTo(4);
        assertThat(request.workspaceEnabled()).isTrue();
        assertThat(request.workspacePath()).isEqualTo("cli-workspace");
        assertThat(request.workspaceMaxEntries()).isEqualTo(5);
        assertThat(request.harnessEnabled()).isTrue();
        assertThat(request.harnessMaxChecks()).isEqualTo(3);
        assertThat(request.harnessIncludeOptional()).isFalse();
    }

    @Test
    void requestOptionsUseSpecPromptWhenCliPromptIsAbsent() {
        AgentRunRequest spec = AgentRunRequest.builder()
                .prompt("spec prompt")
                .systemPrompt("spec system")
                .build();
        WayangRunRequestOptions options = new WayangRunRequestOptions();

        AgentRunRequest request = options.toRequest(spec, InputStream.nullInputStream());

        assertThat(request.prompt()).isEqualTo("spec prompt");
        assertThat(request.systemPrompt()).isEqualTo("spec system");
    }

    @Test
    void requestOptionsRejectBlankProfileIdBeforeReadingSpec() {
        WayangRunRequestOptions options = new WayangRunRequestOptions();
        options.profileId = " ";

        try (WayangClient client = Wayang.client()) {
            assertThatThrownBy(() -> options.readSpecOrDefault(client.specs()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("--profile requires a non-empty id.");
        }
    }
}

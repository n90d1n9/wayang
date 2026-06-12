package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangRunSpecServiceTest {

    private final WayangRunSpecService runSpecs = WayangRunSpecService.create();

    @Test
    void writesAndReadsRunSpec(@TempDir Path workspace) throws Exception {
        Path spec = workspace.resolve("specs").resolve("wayang-run.properties");
        AgentRunRequest request = AgentRunRequest.builder()
                .prompt("answer from service")
                .systemPrompt("stay scoped")
                .tenantId("tenant-a")
                .surfaceId("assistant-agent")
                .skill("rag")
                .context("rag.collection", "docs")
                .build();

        runSpecs.write(spec, request, false);
        AgentRunRequest parsed = runSpecs.read(spec);
        WayangRunSpec parsedSpec = runSpecs.readSpec(spec);

        assertThat(parsedSpec.specVersion()).isEqualTo(WayangRunSpec.CURRENT_VERSION);
        assertThat(parsedSpec.requireReady()).isFalse();
        assertThat(parsed.prompt()).isEqualTo("answer from service");
        assertThat(parsed.systemPrompt()).isEqualTo("stay scoped");
        assertThat(parsed.tenantId()).isEqualTo("tenant-a");
        assertThat(parsed.surfaceId()).isEqualTo("assistant-agent");
        assertThat(parsed.skills()).containsExactly("rag");
        assertThat(parsed.context()).containsEntry("rag.collection", "docs");
    }

    @Test
    void refusesOverwriteUnlessForced(@TempDir Path workspace) throws Exception {
        Path spec = workspace.resolve("wayang-run.properties");
        Files.writeString(spec, "original=true\n");
        AgentRunRequest request = AgentRunRequest.builder()
                .prompt("forced")
                .surfaceId("assistant-agent")
                .build();

        assertThatThrownBy(() -> runSpecs.write(spec, request, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists")
                .hasMessageContaining("--force");
        assertThat(Files.readString(spec)).isEqualTo("original=true\n");

        runSpecs.write(spec, request, true);

        assertThat(Files.readString(spec))
                .contains("specVersion=1" + System.lineSeparator())
                .contains("prompt=forced" + System.lineSeparator())
                .contains("surfaceId=assistant-agent" + System.lineSeparator());
    }

    @Test
    void writesAndReadsLaunchPolicy(@TempDir Path workspace) {
        Path spec = workspace.resolve("strict.properties");
        WayangRunSpec source = WayangRunSpec.of(AgentRunRequest.builder()
                .prompt("strict")
                .surfaceId("assistant-agent")
                .skill("rag")
                .build(), true);

        runSpecs.writeSpec(spec, source, false);
        WayangRunSpec parsed = runSpecs.readSpec(spec);

        assertThat(parsed.requireReady()).isTrue();
        assertThat(parsed.request().prompt()).isEqualTo("strict");
        assertThat(parsed.request().skills()).containsExactly("rag");
    }

    @Test
    void writesSurfaceTemplate(@TempDir Path workspace) throws Exception {
        Path spec = workspace.resolve("assistant.properties");

        runSpecs.writeTemplate(spec, "assistant-agent", false);

        WayangRunSpec template = runSpecs.readSpec(spec);
        AgentRunRequest parsed = runSpecs.read(spec);
        assertThat(template.specVersion()).isEqualTo(WayangRunSpec.CURRENT_VERSION);
        assertThat(template.requireReady()).isFalse();
        assertThat(parsed.surfaceId()).isEqualTo("assistant-agent");
        assertThat(parsed.skills()).containsExactly("memory", "rag", "mcp");
        assertThat(parsed.workspaceEnabled()).isFalse();
        assertThat(parsed.harnessEnabled()).isFalse();
    }

    @Test
    void writesProfileTemplate(@TempDir Path workspace) throws Exception {
        Path spec = workspace.resolve("openclaw.properties");

        runSpecs.writeProfileTemplate(spec, "openclaw-agent", false);

        WayangRunSpec parsed = runSpecs.readSpec(spec);
        assertThat(parsed.profileId()).isEqualTo("openclaw-agent");
        assertThat(parsed.requireReady()).isTrue();
        assertThat(parsed.request().surfaceId()).isEqualTo("coding-agent");
        assertThat(parsed.request().skills()).containsExactly("repo", "tools", "patching", "mcp");
        assertThat(parsed.request().workspaceEnabled()).isTrue();
        assertThat(parsed.request().harnessEnabled()).isTrue();
    }

    @Test
    void readsSpecWithProfileOverride(@TempDir Path workspace) throws Exception {
        Path spec = workspace.resolve("workflow.properties");
        Files.writeString(spec, """
                profileId=assistant-agent
                prompt=wire workflow
                workflowId=custom-flow
                """);

        WayangRunSpec parsed = runSpecs.readSpec(spec, "low-code-agent");

        assertThat(parsed.profileId()).isEqualTo("low-code-agent");
        assertThat(parsed.requireReady()).isTrue();
        assertThat(parsed.request().prompt()).isEqualTo("wire workflow");
        assertThat(parsed.request().surfaceId()).isEqualTo("workflow-platform");
        assertThat(parsed.request().workflowId()).isEqualTo("custom-flow");
        assertThat(parsed.request().skills()).containsExactly("workflow", "hitl", "observability");
    }

    @Test
    void readOrDefaultReturnsEmptyRequestForBlankSpecPath() {
        AgentRunRequest request = runSpecs.readOrDefault(" ");
        WayangRunSpec spec = runSpecs.readSpecOrDefault(" ");

        assertThat(spec.specVersion()).isEqualTo(WayangRunSpec.CURRENT_VERSION);
        assertThat(spec.requireReady()).isFalse();
        assertThat(request.prompt()).isBlank();
        assertThat(request.skills()).isEqualTo(List.of());
    }

    @Test
    void rejectsMissingPaths() {
        assertThatThrownBy(() -> runSpecs.read((String) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path is required");
        assertThatThrownBy(() -> runSpecs.write((Path) null, AgentRunRequest.builder().build(), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path is required");
    }
}

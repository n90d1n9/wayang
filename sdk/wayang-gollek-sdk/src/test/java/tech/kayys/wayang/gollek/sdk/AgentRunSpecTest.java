package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentRunSpecTest {

    @Test
    void mapsPropertiesToAgentRunRequest() {
        Properties properties = new Properties();
        properties.setProperty("prompt", " answer from spec ");
        properties.setProperty("systemPrompt", " stay scoped ");
        properties.setProperty("tenantId", " tenant-a ");
        properties.setProperty("modelId", " model-a ");
        properties.setProperty("workflowId", " workflow-a ");
        properties.setProperty("surfaceId", " assistant-agent ");
        properties.setProperty("sessionId", " session-a ");
        properties.setProperty("userId", " user-a ");
        properties.setProperty("skills", " rag, mcp\nmemory ");
        properties.setProperty("memoryEnabled", "false");
        properties.setProperty("maxSteps", "7");
        properties.setProperty("workspacePath", "/repo");
        properties.setProperty("workspaceEnabled", "true");
        properties.setProperty("workspaceMaxEntries", "25");
        properties.setProperty("harnessEnabled", "true");
        properties.setProperty("harnessMaxChecks", "3");
        properties.setProperty("harnessIncludeOptional", "false");
        properties.setProperty("context.rag.collection", " docs ");
        properties.setProperty("context.mcp.server", " filesystem ");

        AgentRunRequest request = AgentRunSpec.fromProperties(properties);

        assertThat(request.prompt()).isEqualTo("answer from spec");
        assertThat(request.systemPrompt()).isEqualTo("stay scoped");
        assertThat(request.tenantId()).isEqualTo("tenant-a");
        assertThat(request.modelId()).isEqualTo("model-a");
        assertThat(request.workflowId()).isEqualTo("workflow-a");
        assertThat(request.surfaceId()).isEqualTo("assistant-agent");
        assertThat(request.sessionId()).isEqualTo("session-a");
        assertThat(request.userId()).isEqualTo("user-a");
        assertThat(request.skills()).containsExactly("rag", "mcp", "memory");
        assertThat(request.memoryEnabled()).isFalse();
        assertThat(request.maxSteps()).isEqualTo(7);
        assertThat(request.workspaceEnabled()).isTrue();
        assertThat(request.workspacePath()).isEqualTo("/repo");
        assertThat(request.workspaceMaxEntries()).isEqualTo(25);
        assertThat(request.harnessEnabled()).isTrue();
        assertThat(request.harnessMaxChecks()).isEqualTo(3);
        assertThat(request.harnessIncludeOptional()).isFalse();
        assertThat(request.context())
                .containsEntry("rag.collection", "docs")
                .containsEntry("mcp.server", "filesystem");
    }

    @Test
    void formatsPropertiesFromAgentRunRequestDeterministically() {
        AgentRunRequest request = AgentRunRequest.builder()
                .prompt("answer")
                .systemPrompt("stay scoped")
                .tenantId("tenant-a")
                .modelId("model-a")
                .surfaceId("assistant-agent")
                .skill("rag")
                .skill("mcp")
                .workspace("/repo", 25)
                .harness(3, false)
                .context("rag.collection", "docs")
                .context("mcp.server", "filesystem")
                .build();

        String spec = AgentRunSpec.formatProperties(request);

        assertThat(spec)
                .startsWith("prompt=answer" + System.lineSeparator())
                .contains("systemPrompt=stay scoped" + System.lineSeparator())
                .contains("tenantId=tenant-a" + System.lineSeparator())
                .contains("modelId=model-a" + System.lineSeparator())
                .contains("surfaceId=assistant-agent" + System.lineSeparator())
                .contains("skills=rag,mcp" + System.lineSeparator())
                .contains("workspacePath=/repo" + System.lineSeparator())
                .contains("workspaceEnabled=true" + System.lineSeparator())
                .contains("harnessEnabled=true" + System.lineSeparator())
                .contains("harnessIncludeOptional=false" + System.lineSeparator())
                .contains("context.mcp.server=filesystem" + System.lineSeparator())
                .contains("context.rag.collection=docs" + System.lineSeparator());
    }

    @Test
    void formattedPropertiesRoundTripThroughParser() {
        AgentRunRequest source = AgentRunRequest.builder()
                .prompt("answer")
                .tenantId("tenant-a")
                .surfaceId("assistant-agent")
                .skill("rag")
                .context("rag.collection", "docs")
                .build();
        Properties properties = new Properties();

        assertThatCode(() -> properties.load(new java.io.StringReader(AgentRunSpec.formatProperties(source))))
                .doesNotThrowAnyException();
        AgentRunRequest parsed = AgentRunSpec.fromProperties(properties);

        assertThat(parsed.prompt()).isEqualTo("answer");
        assertThat(parsed.tenantId()).isEqualTo("tenant-a");
        assertThat(parsed.surfaceId()).isEqualTo("assistant-agent");
        assertThat(parsed.skills()).containsExactly("rag");
        assertThat(parsed.context()).containsEntry("rag.collection", "docs");
    }

    @Test
    void buildsTemplateFromSurfacePolicy() {
        AgentRunRequest coding = AgentRunSpec.template("coding-agent");

        assertThat(coding.prompt()).isEqualTo("Describe the task here.");
        assertThat(coding.surfaceId()).isEqualTo("coding-agent");
        assertThat(coding.skills()).containsExactly("repo", "tools", "patching");
        assertThat(coding.workspaceEnabled()).isTrue();
        assertThat(coding.harnessEnabled()).isTrue();

        AgentRunRequest assistant = AgentRunSpec.template("assistant-agent");
        assertThat(assistant.surfaceId()).isEqualTo("assistant-agent");
        assertThat(assistant.skills()).containsExactly("memory", "rag", "mcp");
        assertThat(assistant.workspaceEnabled()).isFalse();
        assertThat(assistant.harnessEnabled()).isFalse();
    }

    @Test
    void rejectsInvalidPropertyValues() {
        Properties properties = new Properties();
        properties.setProperty("memoryEnabled", "maybe");

        assertThatThrownBy(() -> AgentRunSpec.fromProperties(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memoryEnabled");

        Properties invalidInt = new Properties();
        invalidInt.setProperty("maxSteps", "0");
        assertThatThrownBy(() -> AgentRunSpec.fromProperties(invalidInt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxSteps");
    }
}

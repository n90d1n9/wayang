package tech.kayys.gamelan.agent.orchestration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class AgentConfigTest {

    @Test
    void defaultsAreReasonable() {
        AgentConfig cfg = AgentConfig.defaults();
        assertThat(cfg.maxIterations()).isEqualTo(10);
        assertThat(cfg.temperature()).isEqualTo(0.7);
        assertThat(cfg.maxTokens()).isEqualTo(4096);
        assertThat(cfg.toolTimeoutSeconds()).isEqualTo(60);
        assertThat(cfg.showMetrics()).isTrue();
        assertThat(cfg.streamOutput()).isTrue();
        assertThat(cfg.systemPromptExtra()).isEmpty();
        assertThat(cfg.model()).isNull();
    }

    @Test
    void loadsFromJsonFile(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("agent.json");
        Files.writeString(file, """
                {
                  "model": "qwen2-7b",
                  "max_iterations": 20,
                  "temperature": 0.3,
                  "tool_timeout_seconds": 120,
                  "system_prompt_extra": "Use records instead of POJOs.",
                  "show_metrics": false
                }
                """);

        AgentConfig cfg = AgentConfig.fromFile(file);

        assertThat(cfg.model()).isEqualTo("qwen2-7b");
        assertThat(cfg.maxIterations()).isEqualTo(20);
        assertThat(cfg.temperature()).isEqualTo(0.3);
        assertThat(cfg.toolTimeoutSeconds()).isEqualTo(120);
        assertThat(cfg.systemPromptExtra()).isEqualTo("Use records instead of POJOs.");
        assertThat(cfg.showMetrics()).isFalse();
    }

    @Test
    void missingFieldsUseDefaults(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("agent.json");
        Files.writeString(file, "{\"model\":\"llama3\"}");
        AgentConfig cfg = AgentConfig.fromFile(file);
        assertThat(cfg.model()).isEqualTo("llama3");
        assertThat(cfg.maxIterations()).isEqualTo(AgentConfig.defaults().maxIterations());
        assertThat(cfg.temperature()).isEqualTo(AgentConfig.defaults().temperature());
    }

    @Test
    void savesToFileAndReloads(@TempDir Path tmp) throws Exception {
        AgentConfig original = AgentConfig.fromFile(writeJson(tmp, """
                {"model":"test-model","max_iterations":15,"temperature":0.4}
                """));
        Path saved = tmp.resolve("saved.json");
        original.saveToFile(saved);
        AgentConfig reloaded = AgentConfig.fromFile(saved);
        assertThat(reloaded.model()).isEqualTo("test-model");
        assertThat(reloaded.maxIterations()).isEqualTo(15);
        assertThat(reloaded.temperature()).isEqualTo(0.4);
    }

    @Test
    void withModelOverridesModel() {
        AgentConfig base = AgentConfig.defaults();
        AgentConfig overridden = base.withModel("qwen2-72b");
        assertThat(overridden.model()).isEqualTo("qwen2-72b");
        assertThat(overridden.maxIterations()).isEqualTo(base.maxIterations());
    }

    @Test
    void withModelNoopOnBlank() {
        AgentConfig base = AgentConfig.defaults();
        assertThat(base.withModel("")).isSameAs(base);
        assertThat(base.withModel(null)).isSameAs(base);
    }

    private Path writeJson(Path dir, String json) throws Exception {
        Path f = dir.resolve("in.json");
        Files.writeString(f, json);
        return f;
    }
}

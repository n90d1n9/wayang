package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCodePromptComposerTest {

    @TempDir
    Path workspace;

    @Test
    void composesWorkspaceAwareCodingAgentSystemPrompt() {
        String prompt = WayangCodePromptComposer.systemPrompt(new WayangCodePromptContext(
                "coding-agent",
                workspace,
                "google/gemma-4-E2B-it",
                true,
                true,
                16));

        assertThat(prompt)
                .contains("You are Wayang Code")
                .contains("Gemini CLI or Claude Code")
                .contains("surface: coding-agent")
                .contains("profile: coding-agent")
                .contains("workspace: " + workspace.toAbsolutePath().normalize())
                .contains("model: google/gemma-4-E2B-it")
                .contains("memory: enabled")
                .contains("harness checks: enabled")
                .contains("max agent steps: 16")
                .contains("Treat natural language such as \"inspect this code\"")
                .contains("Inspect before proposing edits")
                .contains("Always answer with at least one actionable sentence");
    }

    @Test
    void normalizesBlankContextToSafeDefaults() {
        String prompt = WayangCodePromptComposer.systemPrompt(new WayangCodePromptContext(
                " ",
                null,
                " ",
                false,
                false,
                0));

        assertThat(prompt)
                .contains("profile: coding-agent")
                .contains("workspace: " + Path.of(".").toAbsolutePath().normalize())
                .contains("model: default")
                .contains("memory: disabled")
                .contains("harness checks: disabled")
                .contains("max agent steps: 1");
    }
}

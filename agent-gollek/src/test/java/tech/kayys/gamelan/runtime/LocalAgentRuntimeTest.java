package tech.kayys.gamelan.runtime;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link LocalAgentRuntime} — offline simulation, stub injection,
 * tool recording, and assertions.
 */
class LocalAgentRuntimeTest {

    @Test
    void simpleTaskCompletesWithCannedLlmResponse() {
        LocalAgentRuntime runtime = LocalAgentRuntime.builder()
                .stubLlm("The task is complete. No tools needed.")
                .build();

        LocalAgentRuntime.SimulatedResult result = runtime.run("explain what Java is");

        assertThat(result.success()).isTrue();
        assertThat(result.answer()).isEqualTo("The task is complete. No tools needed.");
        assertThat(result.toolCallsMade()).isEmpty();
        assertThat(result.elapsed().toMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void taskWithToolCallInvokesToolStub() {
        LocalAgentRuntime runtime = LocalAgentRuntime.builder()
                .stubLlm(ctx -> ctx.contains("Tool results")
                        ? "Analysis complete based on file."
                        : "<tool_call><n>read_file</n><path>Main.java</path></tool_call>")
                .stubTool("read_file", params -> "public class Main { }")
                .build();

        LocalAgentRuntime.SimulatedResult result = runtime.run("analyze Main.java");

        assertThat(result.toolCallsMade()).anyMatch(c -> c.name().equals("read_file"));
    }

    @Test
    void toolStubReceivesParams() {
        AtomicInteger calls = new AtomicInteger(0);
        LocalAgentRuntime runtime = LocalAgentRuntime.builder()
                .stubLlm("<tool_call><n>counter-tool</n><x>42</x></tool_call>")
                .stubTool("counter-tool", params -> {
                    calls.incrementAndGet();
                    assertThat(params).containsKey("x");
                    return "counted: " + params.get("x");
                })
                .build();

        runtime.run("test");
        assertThat(calls.get()).isGreaterThan(0);
    }

    @Test
    void unknownToolReturnsDefaultOutput() {
        LocalAgentRuntime runtime = LocalAgentRuntime.builder()
                .stubLlm("<tool_call><n>mystery-tool</n></tool_call>")
                // No stub registered for mystery-tool
                .build();

        LocalAgentRuntime.SimulatedResult result = runtime.run("use mystery tool");
        // Should not throw — returns default message
        assertThat(result).isNotNull();
        assertThat(result.toolCallsMade()).anyMatch(c -> c.name().equals("mystery-tool"));
    }

    @Test
    void traceContainsBothLlmAndToolSteps() {
        LocalAgentRuntime runtime = LocalAgentRuntime.builder()
                .stubLlm(ctx -> ctx.contains("Tool results")
                        ? "Done."
                        : "<tool_call><n>read_file</n><path>x</path></tool_call>")
                .stubTool("read_file", "file content")
                .build();

        LocalAgentRuntime.SimulatedResult result = runtime.run("read x");

        assertThat(result.trace()).anyMatch(t -> t.type().equals("LLM_RESPONSE"));
        assertThat(result.trace()).anyMatch(t -> t.type().startsWith("TOOL_CALL:"));
    }

    @Test
    void sessionIsRecordedToDisk(@TempDir Path tmp) throws IOException {
        Path record = tmp.resolve("session.json");
        LocalAgentRuntime runtime = LocalAgentRuntime.builder()
                .stubLlm("Done.")
                .record(record)
                .build();

        runtime.run("test task");

        assertThat(record).exists();
        String content = java.nio.file.Files.readString(record);
        assertThat(content).contains("test task");
        assertThat(content).contains("trace");
    }

    @Test
    void assertionToolCalledPassesWhenToolInvoked() {
        LocalAgentRuntime runtime = LocalAgentRuntime.builder()
                .stubLlm(ctx -> ctx.contains("Tool results")
                        ? "Done."
                        : "<tool_call><n>read_file</n><path>x</path></tool_call>")
                .stubTool("read_file", "content")
                .assertToolCalled("read_file", 1)
                .build();

        LocalAgentRuntime.SimulatedResult result = runtime.run("read x");

        assertThat(result.assertionResults()).allMatch(LocalAgentRuntime.AssertionResult::passed);
    }

    @Test
    void assertionToolCalledFailsWhenToolNotInvoked() {
        LocalAgentRuntime runtime = LocalAgentRuntime.builder()
                .stubLlm("No tools used.")
                .assertToolCalled("write_file", 1) // expects write_file but LLM never calls it
                .build();

        LocalAgentRuntime.SimulatedResult result = runtime.run("some task");

        assertThat(result.assertionResults())
                .anyMatch(a -> !a.passed() && a.name().contains("write_file"));
    }

    @Test
    void assertionOutputContainsPassesWhenMatched() {
        LocalAgentRuntime runtime = LocalAgentRuntime.builder()
                .stubLlm("The answer is 42 as expected.")
                .assertOutputContains("answer is 42")
                .build();

        LocalAgentRuntime.SimulatedResult result = runtime.run("what is the answer?");
        assertThat(result.assertionResults()).allMatch(LocalAgentRuntime.AssertionResult::passed);
    }

    @Test
    void assertionOutputContainsFailsWhenNotMatched() {
        LocalAgentRuntime runtime = LocalAgentRuntime.builder()
                .stubLlm("Something completely different.")
                .assertOutputContains("expected phrase")
                .build();

        LocalAgentRuntime.SimulatedResult result = runtime.run("task");
        assertThat(result.assertionResults())
                .anyMatch(a -> !a.passed() && a.name().contains("expected phrase"));
    }

    @Test
    void maxIterationsIsRespected() {
        AtomicInteger llmCalls = new AtomicInteger(0);
        LocalAgentRuntime runtime = LocalAgentRuntime.builder()
                .stubLlm(ctx -> {
                    llmCalls.incrementAndGet();
                    // Always return a tool call to force more iterations
                    return "<tool_call><n>loop-tool</n></tool_call>";
                })
                .stubTool("loop-tool", "output")
                .maxIterations(3)
                .build();

        runtime.run("infinite loop test");
        assertThat(llmCalls.get()).isLessThanOrEqualTo(3);
    }

    @Test
    void simulatedResultSummaryIsNonBlank() {
        LocalAgentRuntime runtime = LocalAgentRuntime.builder()
                .stubLlm("Completed.")
                .build();
        LocalAgentRuntime.SimulatedResult result = runtime.run("task");
        assertThat(result.summary()).isNotBlank();
    }

    @Test
    void toolCallRecordHasNameAndParams() {
        LocalAgentRuntime runtime = LocalAgentRuntime.builder()
                .stubLlm(ctx -> ctx.contains("Tool results")
                        ? "Done."
                        : "<tool_call><n>read_file</n><path>src/Main.java</path></tool_call>")
                .stubTool("read_file", "class Main{}")
                .build();

        LocalAgentRuntime.SimulatedResult result = runtime.run("read Main.java");

        assertThat(result.toolCallsMade()).isNotEmpty();
        LocalAgentRuntime.ToolCall call = result.toolCallsMade().get(0);
        assertThat(call.name()).isEqualTo("read_file");
        assertThat(call.parameters()).containsKey("path");
    }

    @Test
    void builderFluentApiChains() {
        // Verify the builder returns itself for chaining
        assertThatCode(() ->
                LocalAgentRuntime.builder()
                        .stubLlm("done")
                        .stubTool("t1", "out1")
                        .stubTool("t2", "out2")
                        .maxIterations(5)
                        .verbose(false)
                        .assertToolCalled("t1", 0)
                        .assertOutputContains("done")
                        .build()
        ).doesNotThrowAnyException();
    }
}

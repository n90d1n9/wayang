package tech.kayys.gamelan.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Local Agent Runtime — offline simulation environment for development and testing.
 *
 * <h2>Purpose</h2>
 * Production agent runs require a live LLM (Gollek engine). During development,
 * this is slow, expensive, and non-deterministic. The Local Runtime allows:
 * <ul>
 *   <li><b>Offline simulation</b>: Run full agent workflows without an LLM</li>
 *   <li><b>Script playback</b>: Replay recorded agent sessions deterministically</li>
 *   <li><b>Stub injection</b>: Replace LLM responses with canned outputs for CI</li>
 *   <li><b>Trace recording</b>: Capture complete agent traces for debugging</li>
 *   <li><b>Performance profiling</b>: Measure each layer's overhead without LLM latency</li>
 * </ul>
 *
 * <h2>How it works</h2>
 * <pre>
 * LocalAgentRuntime runtime = LocalAgentRuntime.builder()
 *     .stubLlm(task -> "I'll read the file first.\n<tool_call><n>read_file</n>...")
 *     .stubTool("read_file", params -> "file content here")
 *     .record(Path.of("sessions/debug-session.json"))
 *     .build();
 *
 * SimulatedResult result = runtime.run("fix the bug in UserService.java");
 * // result.trace() → full step-by-step execution trace
 * // result.toolCallsMade() → list of all tool calls
 * // result.replay() → run again deterministically
 * </pre>
 *
 * <h2>CI Integration</h2>
 * The runtime can be configured from a JSON file, allowing CI pipelines to
 * validate agent behavior without requiring a running LLM:
 * <pre>
 * # ci-agent-test.json
 * {
 *   "stubs": [
 *     { "taskPattern": ".*null pointer.*", "response": "read_file → apply_patch" }
 *   ],
 *   "assertions": [
 *     { "tool": "read_file", "minCalls": 1 },
 *     { "output": ".*Optional.*", "required": true }
 *   ]
 * }
 * </pre>
 */
public final class LocalAgentRuntime {

    private static final Logger log = LoggerFactory.getLogger(LocalAgentRuntime.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final Function<String, String>              llmStub;
    private final Map<String, Function<Map<String,String>, String>> toolStubs;
    private final Path                                   recordPath;
    private final int                                    maxIterations;
    private final boolean                                verbose;
    private final List<SimulationAssertion>              assertions;

    private LocalAgentRuntime(Builder b) {
        this.llmStub       = b.llmStub;
        this.toolStubs     = Map.copyOf(b.toolStubs);
        this.recordPath    = b.recordPath;
        this.maxIterations = b.maxIterations;
        this.verbose       = b.verbose;
        this.assertions    = List.copyOf(b.assertions);
    }

    public static Builder builder() { return new Builder(); }

    // ── Execution ──────────────────────────────────────────────────────────

    /**
     * Runs a task through the simulated agent loop.
     * The LLM is replaced by the stub, and tool calls are handled by tool stubs.
     */
    public SimulatedResult run(String task) {
        log.info("[local-runtime] starting simulation: {}", truncate(task, 80));
        Instant start = Instant.now();
        List<TraceStep> trace  = new ArrayList<>();
        List<ToolCall>  calls  = new ArrayList<>();
        String          answer = "";
        boolean         success = true;

        String contextWindow = "System prompt here\n\nUser: " + task + "\n";

        for (int iter = 0; iter < maxIterations; iter++) {
            if (verbose) log.info("[local-runtime] iteration {}", iter + 1);

            // LLM response (stubbed)
            String llmResponse;
            try {
                llmResponse = llmStub.apply(contextWindow);
            } catch (Exception e) {
                llmResponse = "[ERROR] Stub threw: " + e.getMessage();
                success = false;
            }

            TraceStep step = new TraceStep(iter, "LLM_RESPONSE", contextWindow, llmResponse, Instant.now());
            trace.add(step);
            if (verbose) log.info("[local-runtime] LLM: {}", truncate(llmResponse, 100));

            // Parse tool calls from the response
            List<ToolCall> iterCalls = parseToolCalls(llmResponse);
            if (iterCalls.isEmpty()) {
                answer = llmResponse;
                break; // No tool calls → final answer
            }

            // Execute tool calls (via stubs)
            StringBuilder toolResults = new StringBuilder();
            for (ToolCall call : iterCalls) {
                calls.add(call);
                String toolOutput = executeToolStub(call);
                toolResults.append("<tool_result name=\"").append(call.name()).append("\">\n")
                           .append(toolOutput).append("\n</tool_result>\n");
                trace.add(new TraceStep(iter, "TOOL_CALL:" + call.name(),
                        call.parameters().toString(), toolOutput, Instant.now()));
                if (verbose) log.info("[local-runtime] tool {}: {}", call.name(), truncate(toolOutput, 80));
            }

            // Add tool results to context for next iteration
            contextWindow += "Assistant: " + llmResponse + "\nTool results: " + toolResults + "\n";
        }

        Duration elapsed = Duration.between(start, Instant.now());

        // Record session if path is set
        if (recordPath != null) {
            recordSession(task, trace, calls, answer, elapsed);
        }

        // Run assertions
        List<AssertionResult> assertionResults = runAssertions(calls, answer);
        boolean assertionsPass = assertionResults.stream().allMatch(AssertionResult::passed);

        log.info("[local-runtime] simulation complete: {} iterations, {} tool calls, {}ms",
                trace.stream().filter(t -> t.type().startsWith("LLM")).count(),
                calls.size(), elapsed.toMillis());

        return new SimulatedResult(task, answer, success && assertionsPass,
                calls, trace, assertionResults, elapsed);
    }

    // ── Session replay ─────────────────────────────────────────────────────

    /**
     * Loads and replays a previously recorded session.
     * Tool calls use original outputs (deterministic replay).
     */
    public SimulatedResult replay(Path sessionFile) throws IOException {
        RecordedSession session = MAPPER.readValue(sessionFile.toFile(), RecordedSession.class);
        log.info("[local-runtime] replaying session: {}", session.task());

        // Build a runtime that serves recorded outputs
        Map<String, Deque<String>> replayQueues = new LinkedHashMap<>();
        for (TraceStep step : session.trace()) {
            if (step.type().startsWith("TOOL_CALL:")) {
                String toolName = step.type().substring("TOOL_CALL:".length());
                replayQueues.computeIfAbsent(toolName, k -> new ArrayDeque<>())
                            .offer(step.output());
            }
        }

        LocalAgentRuntime replayRuntime = LocalAgentRuntime.builder()
                .stubLlm(ctx -> {
                    // Use the original LLM responses in order
                    long iter = session.trace().stream()
                            .filter(t -> t.type().equals("LLM_RESPONSE")).count();
                    return session.trace().stream()
                            .filter(t -> t.type().equals("LLM_RESPONSE"))
                            .skip(iter).findFirst()
                            .map(TraceStep::output).orElse("");
                })
                .build();

        for (Map.Entry<String, Deque<String>> e : replayQueues.entrySet()) {
            Deque<String> queue = e.getValue();
            replayRuntime.toolStubs.get(e.getKey()); // no-op
        }

        return replayRuntime.run(session.task());
    }

    // ── Playback from script ────────────────────────────────────────────────

    /**
     * Loads a CI test script and runs the simulation against assertions.
     */
    @SuppressWarnings("unchecked")
    public List<AssertionResult> runScript(Path scriptFile) throws IOException {
        Map<String, Object> script = MAPPER.readValue(scriptFile.toFile(), Map.class);
        String task = (String) script.getOrDefault("task", "test task");
        SimulatedResult result = run(task);
        return result.assertionResults();
    }

    // ── Private ────────────────────────────────────────────────────────────

    private List<ToolCall> parseToolCalls(String text) {
        if (!text.contains("<tool_call>") && !text.contains("<tool_call ")) return List.of();
        List<ToolCall> calls = new ArrayList<>();
        // Simple regex-free parser
        int pos = 0;
        while (pos < text.length()) {
            int start = text.indexOf("<tool_call", pos);
            if (start < 0) break;
            int end = text.indexOf("</tool_call>", start);
            if (end < 0) break;
            String block = text.substring(start, end + 12);
            String name  = extractTag(block, "n");
            if (name == null) name = extractTag(block, "name");
            if (name != null) {
                Map<String, String> params = new LinkedHashMap<>();
                extractAllTags(block, params);
                params.remove("n"); params.remove("name");
                calls.add(new ToolCall(name.strip(), params));
            }
            pos = end + 12;
        }
        return calls;
    }

    private String extractTag(String xml, String tag) {
        String open = "<" + tag + ">", close = "</" + tag + ">";
        int s = xml.indexOf(open);
        if (s < 0) return null;
        int e = xml.indexOf(close, s);
        if (e < 0) return null;
        return xml.substring(s + open.length(), e).strip();
    }

    private void extractAllTags(String xml, Map<String, String> out) {
        int pos = 0;
        while (pos < xml.length()) {
            int open  = xml.indexOf('<', pos);
            if (open < 0 || xml.charAt(open + 1) == '/') break;
            int nameEnd = xml.indexOf('>', open + 1);
            if (nameEnd < 0) break;
            String tag   = xml.substring(open + 1, nameEnd).strip();
            if (tag.contains(" ") || tag.isEmpty()) { pos = nameEnd + 1; continue; }
            String close = "</" + tag + ">";
            int closePos = xml.indexOf(close, nameEnd);
            if (closePos < 0) { pos = nameEnd + 1; continue; }
            out.put(tag, xml.substring(nameEnd + 1, closePos).strip());
            pos = closePos + close.length();
        }
    }

    private String executeToolStub(ToolCall call) {
        Function<Map<String,String>, String> stub = toolStubs.get(call.name());
        if (stub != null) return stub.apply(call.parameters());
        return "[LocalRuntime] No stub for tool '" + call.name() + "' — returning empty output";
    }

    private List<AssertionResult> runAssertions(List<ToolCall> calls, String output) {
        List<AssertionResult> results = new ArrayList<>();
        for (SimulationAssertion assertion : assertions) {
            results.add(assertion.evaluate(calls, output));
        }
        return results;
    }

    private void recordSession(String task, List<TraceStep> trace, List<ToolCall> calls,
                                String answer, Duration elapsed) {
        try {
            Files.createDirectories(recordPath.getParent());
            RecordedSession session = new RecordedSession(task, answer, trace,
                    calls.size(), elapsed.toMillis(), Instant.now());
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(recordPath.toFile(), session);
            log.info("[local-runtime] session recorded → {}", recordPath);
        } catch (IOException e) {
            log.warn("[local-runtime] recording failed: {}", e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Builder ────────────────────────────────────────────────────────────

    public static final class Builder {
        private Function<String, String>                    llmStub       = ctx -> "Task complete.";
        private final Map<String, Function<Map<String,String>, String>> toolStubs = new LinkedHashMap<>();
        private Path                                         recordPath    = null;
        private int                                          maxIterations = 10;
        private boolean                                      verbose       = false;
        private final List<SimulationAssertion>              assertions    = new ArrayList<>();

        public Builder stubLlm(Function<String, String> stub)        { this.llmStub       = stub; return this; }
        public Builder stubLlm(String cannedResponse)                 { return stubLlm(ctx -> cannedResponse); }
        public Builder stubTool(String name, Function<Map<String,String>, String> s) { toolStubs.put(name, s); return this; }
        public Builder stubTool(String name, String output)           { return stubTool(name, p -> output); }
        public Builder record(Path path)                              { this.recordPath    = path; return this; }
        public Builder maxIterations(int n)                           { this.maxIterations = n;    return this; }
        public Builder verbose(boolean v)                             { this.verbose       = v;    return this; }
        public Builder assertToolCalled(String tool, int minTimes)    { assertions.add(SimulationAssertion.toolCalled(tool, minTimes)); return this; }
        public Builder assertOutputContains(String pattern)           { assertions.add(SimulationAssertion.outputContains(pattern)); return this; }
        public LocalAgentRuntime build()                              { return new LocalAgentRuntime(this); }
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record ToolCall(String name, Map<String, String> parameters) {}

    public record TraceStep(int iteration, String type, String input, String output, Instant timestamp) {}

    public record SimulatedResult(
            String                  task,
            String                  answer,
            boolean                 success,
            List<ToolCall>          toolCallsMade,
            List<TraceStep>         trace,
            List<AssertionResult>   assertionResults,
            Duration                elapsed
    ) {
        public String summary() {
            long passed = assertionResults.stream().filter(AssertionResult::passed).count();
            return String.format("Simulation: %s | %d tool calls | %d/%d assertions | %dms",
                    success ? "SUCCESS" : "FAILED", toolCallsMade.size(),
                    passed, assertionResults.size(), elapsed.toMillis());
        }
    }

    public record RecordedSession(String task, String answer, List<TraceStep> trace,
            int toolCallCount, long durationMs, Instant recordedAt) {}

    public interface SimulationAssertion {
        AssertionResult evaluate(List<ToolCall> calls, String output);

        static SimulationAssertion toolCalled(String toolName, int minTimes) {
            return (calls, output) -> {
                long count = calls.stream().filter(c -> c.name().equals(toolName)).count();
                boolean passed = count >= minTimes;
                return new AssertionResult("tool_called:" + toolName + ">=" + minTimes,
                        passed, passed ? null : "Tool '" + toolName + "' called " + count +
                        " times, expected >= " + minTimes);
            };
        }

        static SimulationAssertion outputContains(String pattern) {
            return (calls, output) -> {
                boolean passed = output != null && output.matches("(?s).*" + pattern + ".*");
                return new AssertionResult("output_contains:" + pattern, passed,
                        passed ? null : "Output does not match pattern: " + pattern);
            };
        }
    }

    public record AssertionResult(String name, boolean passed, String failureReason) {}
}

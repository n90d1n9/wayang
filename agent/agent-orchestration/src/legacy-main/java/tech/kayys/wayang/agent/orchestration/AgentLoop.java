package tech.kayys.wayang.agent.orchestration;

import tech.kayys.gollek.tools.*;
import tech.kayys.gollek.sdk.local.GollekLocalClient;
import tech.kayys.gollek.sdk.local.LocalGollekSdk;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.gollek.spi.tool.ToolDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Core agentic loop — drives the plan→tool→observe→repeat cycle.
 *
 * Features:
 *   - Streaming (real-time token output) or blocking mode
 *   - Parallel tool execution when Claude emits multiple tool-use blocks simultaneously
 *   - Per-tool execution metrics (latency, errors, call count)
 *   - SIGINT (Ctrl+C) graceful shutdown
 *   - Checkpoint: save/restore conversation state to disk
 *   - Post-run usage + metrics summary
 */
public class AgentLoop {

    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);

    private final AgentConfig        config;
    private final GollekLocalClient  client;
    private final ToolRegistry       registry;
    private final ConversationMemory memory;
    private final ToolMetrics        metrics;
    private final AtomicBoolean      interrupted = new AtomicBoolean(false);
    // ── Event hooks ───────────────────────────────────────────

    /**
     * Implement this interface to receive callbacks during the agent loop.
     * Useful for progress tracking, custom logging, or UI integration.
     */
    public interface AgentEventListener {
        default void onIterationStart(int iteration, int maxIterations) {}
        default void onIterationEnd(int iteration, String stopReason) {}
        default void onToolStart(String toolName, com.fasterxml.jackson.databind.JsonNode input) {}
        default void onToolEnd(String toolName, String result, boolean error, long durationMs) {}
        default void onTextOutput(String text) {}
        default void onComplete(String finalAnswer, int iterations) {}
        default void onError(Exception e, int iteration) {}
    }

    private AgentEventListener eventListener = new AgentEventListener() {}; // no-op default

    public void setEventListener(AgentEventListener listener) {
        this.eventListener = listener != null ? listener : new AgentEventListener() {};
    }



    // ── System prompt ─────────────────────────────────────────
    static final String SYSTEM_PROMPT_BASE = """
            You are an expert autonomous coding agent specializing in Java and software engineering.
            You operate with full agentic autonomy — plan, act, verify, fix, and iterate until done.

            You have access to a variety of tools across reasoning, filesystem, shell, Java toolchain,
            testing, build & CI, network, data, and more. Use them appropriately to solve the task.

            ## Agent Operating Principles
            1. **Always think first** for complex tasks — use the think tool
            2. **Read before writing** — use tools to understand existing code
            3. **Prefer patch_file** over write_file for editing existing files
            4. **Compile early** — use compile_java after writing Java to catch errors fast
            5. **Test after changes** — run tests to verify correctness
            6. **Verify with tools** — don't assume; use file_exists, health_check, etc.
            7. **Handle errors** — retry with different approach; explain what failed
            8. **Small steps** — break large tasks into verifiable increments
            9. **Use the right tool** — don't shell out for things dedicated tools handle better
            10. **Concise summary** — the user sees all tool output; your final text should summarize only.
            """;

    // ── Constructor ───────────────────────────────────────────

    public AgentLoop(AgentConfig config) {
        this.config   = config;
        this.client   = new LocalGollekSdk(); // Initialize Gollek SDK
        this.memory   = new ConversationMemory(50, 80_000); // 50 turns, ~80k token budget
        this.metrics  = new ToolMetrics();
        this.registry = buildRegistry(config);
        installShutdownHook();
    }

    // ── Public API ────────────────────────────────────────────

    /**
     * Run the agent on a task. Blocks until complete, interrupted, or maxIterations reached.
     */
    public String run(String task) throws Exception {
        interrupted.set(false);
        memory.clear();
        metrics.reset();
        memory.addUser(task);

        List<ToolDefinition> toolDefs = registry.toGollekToolDefinitions();
        String systemPrompt = buildSystemPrompt();

        int    iteration = 0;
        String lastText  = "";

        while (iteration < config.maxIterations() && !interrupted.get()) {
            iteration++;
            log.info("[agent] Iteration {}/{}", iteration, config.maxIterations());
            eventListener.onIterationStart(iteration, config.maxIterations());

            // ── Call the Gollek API ───────────────────────────
            InferenceResponse response;
            if (config.streamOutput()) {
                System.out.println("  ⚠️  Streaming not fully implemented — falling back to blocking.");
                response = callGollekBlocking(memory.getMessages(), toolDefs, systemPrompt);
                lastText = response.getContent();
            } else {
                response = callGollekBlocking(memory.getMessages(), toolDefs, systemPrompt);
                lastText = response.getContent();
                if (!lastText.isBlank()) {
                    System.out.println("\n🤖 " + lastText);
                    eventListener.onTextOutput(lastText);
                }
            }

            String stopReason = response.getFinishReason().name().toLowerCase();
            eventListener.onIterationEnd(iteration, stopReason);

            // ── Finished ──────────────────────────────────────
            if ("stop".equals(stopReason)) {
                log.info("[agent] Done after {} iterations", iteration);
                eventListener.onComplete(lastText, iteration);
                printSummary();
                return lastText;
            }

            // ── Execute tools (in parallel when possible) ─────
            if ("tool_calls".equals(stopReason) && response.hasToolCalls()) {
                List<InferenceResponse.ToolCall> toolCalls = response.getToolCalls();

                if (toolCalls.size() == 1) {
                    // Common case: single tool — execute directly
                    executeSingleToolGollek(toolCalls.get(0));
                } else {
                    // Multiple tools: execute in parallel
                    executeToolsParallelGollek(toolCalls);
                }
                continue;
            }

            if ("length".equals(stopReason)) {
                // Model hit the token limit mid-response — add a continuation nudge
                log.warn("[agent] Hit max_tokens limit on iteration {}", iteration);
                System.out.println("  ⚠️  Token limit reached — asking model to continue...");
                memory.addUser("You were cut off by the token limit. Please continue from where you left off, completing any unfinished work.");
                continue;
            }
            log.warn("[agent] Unexpected stop_reason: {}", stopReason);
            break;
        }

        if (interrupted.get()) {
            System.out.println("\n⚡ Agent interrupted by user.");
        } else {
            System.out.println("\n⚠️  Reached max iterations (" + config.maxIterations() + ").");
        }
        printSummary();
        return lastText;
    }

    // ── Checkpoint ────────────────────────────────────────────

    /** Save conversation state for later resumption. */
    public void saveCheckpoint(Path path) throws Exception {
        log.info("[agent] Checkpoint saving not implemented in this version");
    }
 
    /** Restore conversation state and continue from where we left off. */
    public void loadCheckpoint(Path path) throws Exception {
        log.info("[agent] Checkpoint loading not implemented in this version");
    }

    // ── Accessors ─────────────────────────────────────────────

    public ToolMetrics        getMetrics()  { return metrics; }
    public GollekLocalClient  getClient()   { return client; }
    public ToolRegistry       getRegistry() { return registry; }
    public ConversationMemory getMemory()   { return memory; }
    public void               interrupt()   { interrupted.set(true); }

    // ── Tool execution ────────────────────────────────────────

    private void executeSingleToolGollek(InferenceResponse.ToolCall toolCall) throws Exception {
        String toolName = toolCall.name();
        Map<String, Object> input = toolCall.arguments();

        System.out.printf("%n  ⚡ [%s]%n", toolName);
        eventListener.onToolStart(toolName, convertToJsonNode(input));
        long t0 = metrics.startTimer();
        boolean error = false;
        String result;

        try {
            result = registry.getAgentTool(toolName).execute(convertToJsonNode(input));
        } catch (IllegalArgumentException e) {
            result = "ERROR: Unknown tool '" + toolName + "'. Check tool name.";
            error  = true;
            eventListener.onError(e, 0);
        } catch (Exception e) {
            result = "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            error  = true;
            log.error("[agent] Tool {} failed", toolName, e);
            eventListener.onError(e, 0);
        }

        long elapsed = System.currentTimeMillis() - t0;
        metrics.record(toolName, t0, error);
        eventListener.onToolEnd(toolName, result, error, elapsed);
        memory.addToolResult(toolName, truncate(result));
    }

    private void executeToolsParallelGollek(List<InferenceResponse.ToolCall> toolCalls) throws Exception {
        log.info("[agent] Executing {} tools in parallel", toolCalls.size());
        System.out.printf("%n  ⚡ Executing %d tools in parallel%n", toolCalls.size());

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(toolCalls.size(), 4));
        List<Future<ToolResult>> futures = new ArrayList<>();

        for (InferenceResponse.ToolCall toolCall : toolCalls) {
            String toolName = toolCall.name();
            Map<String, Object> input = toolCall.arguments();

            futures.add(pool.submit(() -> {
                System.out.printf("  ⚡ [%s]%n", toolName);
                long t0 = metrics.startTimer();
                boolean err = false;
                String result;
                eventListener.onToolStart(toolName, convertToJsonNode(input));
                try {
                    result = registry.getAgentTool(toolName).execute(convertToJsonNode(input));
                } catch (Exception e) {
                    result = "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
                    err    = true;
                    log.error("[agent] Parallel tool {} failed", toolName, e);
                    eventListener.onError(e, 0);
                }
                long elapsed = System.currentTimeMillis() - t0;
                metrics.record(toolName, t0, err);
                eventListener.onToolEnd(toolName, result, err, elapsed);
                return new ToolResult(toolName, toolName, result, err);
            }));
        }

        pool.shutdown();
        pool.awaitTermination(config.toolTimeoutSeconds() * (long) toolCalls.size(), TimeUnit.SECONDS);

        // Collect results in original order and add to memory
        for (Future<ToolResult> future : futures) {
            ToolResult tr = future.get();
            memory.addToolResult(tr.toolId(), truncate(tr.result()));
        }
    }

    private record ToolResult(String toolId, String toolName, String result, boolean error) {}

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Call Gollek SDK for inference (blocking mode).
     */
    private InferenceResponse callGollekBlocking(
            List<Message> messages,
            List<ToolDefinition> tools,
            String systemPrompt) throws Exception {
        
        // Add system message to the beginning if not present
        List<Message> enrichedMessages = new ArrayList<>(messages);
        if (enrichedMessages.isEmpty() || enrichedMessages.get(0).getRole() != Message.Role.SYSTEM) {
            enrichedMessages.add(0, Message.system(systemPrompt));
        }

        InferenceRequest request = InferenceRequest.builder()
                .model(config.model())
                .messages(enrichedMessages)
                .tools(tools)
                .maxTokens(config.maxTokens())
                .streaming(false)
                .build();

        return client.createCompletion(request);
    }

    /**
     * Convert a Map to JsonNode for tool execution compatibility.
     */
    private com.fasterxml.jackson.databind.JsonNode convertToJsonNode(Map<String, Object> map) {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.valueToTree(map);
    }

    private String buildSystemPrompt() {
        String extra = config.systemPromptExtra();
        return SYSTEM_PROMPT_BASE
                + (extra == null || extra.isBlank() ? ""
                : "\n\n## Additional Instructions\n" + extra);
    }

    private String truncate(String result) {
        if (result == null) return "(no output)";
        if (result.length() <= 12_000) return result;
        return result.substring(0, 6000)
                + "\n\n...[TRUNCATED — " + result.length() + " chars total]...\n\n"
                + result.substring(result.length() - 4000);
    }

    private void printSummary() {
        System.out.println("\n" + "─".repeat(62));
        System.out.println("Usage summary not available");
        System.out.println("Memory stats not available");
        if (config.showMetrics() && !metrics.toolNames().isEmpty()) {
            System.out.println();
            System.out.print(metrics.summary());
        }
        System.out.println("─".repeat(62));
    }

    private void installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!interrupted.get()) {
                interrupted.set(true);
                System.out.println("\n[agent] Shutdown signal received — stopping after current tool.");
            }
        }));
    }

    // ── Tool registration ─────────────────────────────────────

    private ToolRegistry buildRegistry(AgentConfig config) {
        ToolRegistry r = new ToolRegistry();

        // Load all AgentTool implementations via ServiceLoader
        r.loadPlugins(null);

        // Also register CodeTool instances if they are available
        // (This would be handled by loadPlugins if they are adapted to AgentTool)

        return r;
    }
}

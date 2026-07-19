package tech.kayys.wayang.agent;

import tech.kayys.wayang.json.JsonValue;
import tech.kayys.wayang.provider.ChatMessage;
import tech.kayys.wayang.provider.ContentBlock;
import tech.kayys.wayang.provider.Provider;
import tech.kayys.wayang.provider.StreamEvent;
import tech.kayys.wayang.provider.ToolSpec;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.tools.spi.ToolContext;

import java.util.*;
import java.util.concurrent.*;

/**
 * Core agentic loop for the Wayang platform.
 *
 * <p>Drives one user turn to completion: sends the conversation to the provider,
 * streams the response, and — if the model requests tool calls — executes them
 * (after an optional permission check for mutating tools) and loops back with the
 * results until the model produces a final answer with no further tool use.
 *
 * <p>The {@link Tool} implementations come from the official
 * {@code tech.kayys.wayang.tools.spi.Tool} SPI (e.g. {@code wayang-tool-os},
 * {@code wayang-tool-mcp}), keeping the agent loop decoupled from specific
 * tool implementations.
 *
 * <p>Call {@link #send(String, WayangAgentListener)} on a background thread; it
 * blocks for the whole multi-step turn, including waiting on any permission prompts.
 */
public final class WayangAgent {

    private volatile Provider provider;

    /**
     * Indexed by {@link Tool#id()} for O(1) lookup during tool dispatch.
     * Volatile reference so setTools() is thread-safe at the reference level;
     * do not mutate the map after construction.
     */
    private volatile Map<String, Tool> toolIndex;

    private volatile String systemPrompt;
    private final double temperature;
    private final int maxTokens;
    private volatile boolean autoApproveTools;
    private final java.nio.file.Path workspace;

    private final List<ChatMessage> history = new ArrayList<>();
    private final Set<String> sessionApprovedTools = ConcurrentHashMap.newKeySet();
    private final WayangSessionPersistence sessionPersistence = new WayangSessionPersistence();

    /** Hard limit on agentic loops per turn to prevent runaway chains. */
    private static final int MAX_TOOL_ITERATIONS = 50;

    public WayangAgent(Provider provider,
                       List<Tool> tools,
                       String systemPrompt,
                       double temperature,
                       int maxTokens,
                       boolean autoApproveTools,
                       java.nio.file.Path workspace) {
        this.provider = provider;
        this.toolIndex = indexTools(tools);
        this.systemPrompt = systemPrompt;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.autoApproveTools = autoApproveTools;
        this.workspace = workspace != null ? workspace : java.nio.file.Path.of(System.getProperty("user.dir"));
    }

    public void setModelId(String modelId) {
        if (this.provider != null) {
            try {
                this.provider.getClass().getMethod("setModelId", String.class).invoke(this.provider, modelId);
            } catch (Exception e) {
                // Ignore if provider doesn't support changing model
            }
        }
    }

    public String getModelId() {
        if (this.provider != null) {
            try {
                return (String) this.provider.getClass().getMethod("getModelId").invoke(this.provider);
            } catch (Exception e) {
                // Ignore if provider doesn't support changing model
            }
        }
        return null;
    }

    // ── Accessors ───────────────────────────────────────────────────────────

    public List<ChatMessage> history()          { return history; }
    public void clearHistory()                  { history.clear(); sessionApprovedTools.clear(); persistHistory(); }

    /**
     * Replace the current conversation history with the provided messages.
     * Used when switching projects or resuming a different session mid-session.
     * Clears session-approved tools to avoid carrying over approval grants.
     */
    public void replaceHistory(List<ChatMessage> newHistory) {
        history.clear();
        sessionApprovedTools.clear();
        if (newHistory != null) history.addAll(newHistory);
        persistHistory();
    }
    
    private void persistHistory() {
        try {
            sessionPersistence.save(history);
        } catch (Exception e) {
            // Silently fail - don't interrupt the conversation flow
        }
    }

    public java.nio.file.Path workspace() {
        return workspace;
    }

    public Collection<Tool> tools()             { return toolIndex.values(); }
    public void setTools(List<Tool> tools)      { this.toolIndex = indexTools(tools); }

    public boolean autoApproveTools()           { return autoApproveTools; }
    public void setAutoApproveTools(boolean v)  { this.autoApproveTools = v; }

    public void setProvider(Provider provider)  { this.provider = provider; }
    public void setSystemPrompt(String sp)      { this.systemPrompt = sp; }

    // ── Public API ──────────────────────────────────────────────────────────

    public void send(String userInput, WayangAgentListener listener) {
        try {
            java.nio.file.Files.write(
                java.nio.file.Path.of(System.getProperty("user.home"), ".wayang", "sessions", "debug-agent.log"),
                ("[WayangAgent.send] called with: " + (userInput == null ? "null" : userInput) + "\n").getBytes(),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) { }
        
        history.add(ChatMessage.userText(userInput));
        persistHistory();
        runUntilDone(listener);
    }

    // ── Internal loop ───────────────────────────────────────────────────────

    private void runUntilDone(WayangAgentListener listener) {
        // Build provider-agnostic ToolSpec list from official Tool SPI.
        List<ToolSpec> toolSpecs = buildToolSpecs();

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            if (Thread.currentThread().isInterrupted()) {
                listener.onError("Interrupted by user.");
                return;
            }
            TurnAccumulator acc = new TurnAccumulator();

            try {
                provider.streamChat(history, systemPrompt, toolSpecs, temperature, maxTokens,
                        event -> handleEvent(event, acc, listener));
            } catch (Exception e) {
                listener.onError("Request failed: " + describeError(e));
                return;
            }

            if (acc.errored) return;

            // Commit assistant turn to history.
            List<ContentBlock> blocks = new ArrayList<>();
            if (!acc.text.isEmpty()) blocks.add(new ContentBlock.Text(acc.text.toString()));
            blocks.addAll(acc.toolUses);
            if (!blocks.isEmpty()) {
                history.add(ChatMessage.assistant(blocks));
                persistHistory();
            }

            boolean wantsTools = !acc.toolUses.isEmpty();
            if (!wantsTools) {
                listener.onDone(acc.stopReason == null ? "end_turn" : acc.stopReason);
                return;
            }

            // Execute requested tool calls, gating mutating ones behind permission.
            List<ContentBlock.ToolResult> results = new ArrayList<>();
            for (ContentBlock.ToolUse call : acc.toolUses) {
                listener.onToolCallReady(call.id(), call.name(), call.input());

                Tool tool = toolIndex.get(call.name());
                if (tool == null) {
                    results.add(new ContentBlock.ToolResult(call.id(), "Unknown tool: " + call.name(), true));
                    continue;
                }

                // Permission gate for mutating tools.
                if (!autoApproveTools && !sessionApprovedTools.contains(tool.id())) {
                    PermissionDecision decision = awaitPermission(call, listener);
                    switch (decision) {
                        case DENY -> {
                            results.add(new ContentBlock.ToolResult(call.id(),
                                    "User denied permission to run this tool.", true));
                            continue;
                        }
                        case APPROVE_ALWAYS_THIS_TOOL -> sessionApprovedTools.add(tool.id());
                        default -> {} // APPROVE_ONCE — proceed
                    }
                }

                // Dispatch: bridge JsonValue -> Map<String, Object> for the Tool SPI.
                Map<String, Object> params = call.input() != null
                        ? call.input().asStringObjectMap()
                        : Map.of();
                ToolContext ctx = ToolContext.withDirectory(workspace);
                ToolResult result = tool.execute(params, ctx);
                listener.onToolResult(call.id(), call.name(), result);

                String content = result.success()
                        ? (result.data() instanceof String s ? s : Objects.toString(result.data()))
                        : result.error();
                results.add(new ContentBlock.ToolResult(call.id(), content, !result.success()));
            }

            history.add(ChatMessage.toolResults(results));
        }

        listener.onError("Stopped after " + MAX_TOOL_ITERATIONS + " tool-use iterations without a final answer.");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Build provider-agnostic ToolSpec list from the official Tool SPI. */
    private List<ToolSpec> buildToolSpecs() {
        if (toolIndex.isEmpty()) return List.of();
        List<ToolSpec> specs = new ArrayList<>(toolIndex.size());
        for (Tool t : toolIndex.values()) {
            specs.add(new ToolSpec(t.id(), t.description(), t.inputSchema()));
        }
        return specs;
    }

    private static Map<String, Tool> indexTools(List<Tool> tools) {
        Map<String, Tool> index = new LinkedHashMap<>();
        for (Tool t : tools) index.put(t.id(), t);
        return Collections.unmodifiableMap(index);
    }

    private static String describeError(Exception e) {
        String msg = e.getMessage();
        return (msg != null && !msg.isBlank()) ? msg : e.getClass().getSimpleName();
    }

    private PermissionDecision awaitPermission(ContentBlock.ToolUse call, WayangAgentListener listener) {
        CompletableFuture<PermissionDecision> future = new CompletableFuture<>();
        listener.onToolPermissionNeeded(call.id(), call.name(), call.input(), future::complete);
        try {
            return future.get();
        } catch (Exception e) {
            return PermissionDecision.DENY;
        }
    }

    // ── Event handling ───────────────────────────────────────────────────────

    private void handleEvent(StreamEvent event, TurnAccumulator acc, WayangAgentListener listener) {
        switch (event) {
            case StreamEvent.TextDelta(String t) -> {
                acc.text.append(t);
                listener.onTextDelta(t);
            }
            case StreamEvent.ThinkingDelta(String t) -> listener.onThinkingDelta(t);
            case StreamEvent.ThinkingEnd()           -> listener.onThinkingEnd();
            case StreamEvent.ToolUseStart(String id, String name) -> {
                acc.pendingToolNames.put(id, name);
                listener.onToolCallStart(id, name);
            }
            case StreamEvent.ToolUseInputDelta ignored -> {}
            case StreamEvent.ToolUseEnd(String id, JsonValue input) -> {
                String name = acc.pendingToolNames.getOrDefault(id, "unknown");
                acc.toolUses.add(new ContentBlock.ToolUse(id, name, input));
            }
            case StreamEvent.Usage(int in, int out) -> listener.onUsage(in, out);
            case StreamEvent.MessageStop(String reason) -> acc.stopReason = reason;
            case StreamEvent.Error(String message) -> {
                acc.errored = true;
                listener.onError(message);
            }
        }
    }

    // ── Inner types ──────────────────────────────────────────────────────────

    private static final class TurnAccumulator {
        final StringBuilder text = new StringBuilder();
        final List<ContentBlock.ToolUse> toolUses = new ArrayList<>();
        final Map<String, String> pendingToolNames = new HashMap<>();
        String stopReason;
        boolean errored = false;
    }
}

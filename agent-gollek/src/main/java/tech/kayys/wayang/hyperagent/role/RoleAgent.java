package tech.kayys.gamelan.hyperagent.role;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.role.AgentRole;
import tech.kayys.gamelan.communication.AgentMessageBus;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * RoleAgent — a single agent instance bound to an {@link AgentRole}.
 *
 * <h2>What makes a RoleAgent different from a plain agent</h2>
 * A plain {@link tech.kayys.gamelan.agent.orchestration.SingleAgentOrchestrator} is stateless
 * and role-agnostic. A RoleAgent:
 * <ul>
 *   <li>Has a persistent identity (ID, name, role) across the multi-agent session</li>
 *   <li>Has constrained tools — it can ONLY call tools permitted by its role</li>
 *   <li>Has a role-specific system prompt injected before every turn</li>
 *   <li>Publishes observations to the shared {@link AgentMessageBus}</li>
 *   <li>Subscribes to messages directed at it or its role</li>
 *   <li>Maintains a turn-level conversation history isolated from other agents</li>
 *   <li>Reports its state (IDLE / THINKING / ACTING / WAITING / DONE / FAILED)</li>
 * </ul>
 *
 * <h2>Turn execution</h2>
 * Each {@link #turn(String)} call is a single reasoning step:
 * <pre>
 * 1. Build system prompt = role persona + tool catalogue + shared context
 * 2. Call LLM with conversation history
 * 3. Parse any tool calls from the response
 * 4. Execute permitted tools (reject forbidden ones)
 * 5. Publish findings to message bus
 * 6. Return the agent's response
 * </pre>
 *
 * <h2>Thread safety</h2>
 * Each RoleAgent runs on a single virtual thread. Multiple RoleAgents can run
 * in parallel — they communicate only through the {@link AgentMessageBus} and
 * the shared blackboard, never through shared mutable state.
 */
public final class RoleAgent {

    private static final Logger log = LoggerFactory.getLogger(RoleAgent.class);

    private static final int MAX_TOOL_CALLS_PER_TURN = 10;
    private static final int MAX_TURNS               = 20;

    private final String         agentId;
    private final String         name;
    private final AgentRole      role;
    private final GollekSdk      sdk;
    private final GamelanConfig  config;
    private final AgentMessageBus bus;
    private final String         model;

    // Conversation state (per-agent, not shared)
    private final List<Message>           history     = new ArrayList<>();
    private final List<TurnRecord>        turns       = new ArrayList<>();
    private final AtomicReference<State>  state       = new AtomicReference<>(State.IDLE);
    private final AtomicInteger           turnCount   = new AtomicInteger(0);

    // Tool execution (only permitted tools allowed)
    private final Map<String, java.util.function.Function<Map<String,String>, String>> toolStubs
            = new ConcurrentHashMap<>();

    private RoleAgent(Builder b) {
        this.agentId = b.agentId;
        this.name    = b.name;
        this.role    = b.role;
        this.sdk     = b.sdk;
        this.config  = b.config;
        this.bus     = b.bus;
        this.model   = b.model != null ? b.model : (config != null ? config.defaultModel() : "llama3");
    }

    public static Builder builder(String agentId, AgentRole role) {
        return new Builder(agentId, role);
    }

    // ── Core execution ─────────────────────────────────────────────────────

    /**
     * Executes a single reasoning turn for this agent.
     *
     * @param input  the task or message directed to this agent
     * @return the agent's response text
     */
    public TurnResult turn(String input) {
        if (turnCount.get() >= MAX_TURNS) {
            return TurnResult.maxTurnsReached(agentId, role, turnCount.get());
        }
        state.set(State.THINKING);
        Instant start = Instant.now();

        try {
            // Build the full system prompt for this role
            String systemPrompt = buildSystemPrompt(input);

            // Add user message to history
            history.add(Message.user(input));

            // Call LLM
            String response = callLlm(systemPrompt);

            // Parse and execute tool calls
            List<ToolExecution> executions = new ArrayList<>();
            String finalResponse = response;

            List<ParsedToolCall> toolCalls = parseToolCalls(response);
            if (!toolCalls.isEmpty()) {
                state.set(State.ACTING);
                StringBuilder toolResultsBlock = new StringBuilder();

                for (ParsedToolCall tc : toolCalls) {
                    if (executions.size() >= MAX_TOOL_CALLS_PER_TURN) break;

                    ToolExecution exec = executeToolCall(tc);
                    executions.add(exec);
                    toolResultsBlock.append(exec.toXml()).append("\n");
                }

                // Add tool results to history and get final response
                history.add(Message.assistant(response));
                history.add(Message.user(toolResultsBlock.toString()));
                finalResponse = callLlm(systemPrompt);
            }

            // Add final assistant response to history
            history.add(Message.assistant(finalResponse));

            Duration elapsed = Duration.between(start, Instant.now());
            int turn = turnCount.incrementAndGet();

            TurnRecord record = new TurnRecord(turn, input, response, finalResponse,
                    executions, elapsed, Instant.now());
            turns.add(record);

            // Publish findings to message bus
            if (bus != null) {
                publishFindings(finalResponse, executions);
            }

            state.set(executions.isEmpty() ? State.IDLE : State.DONE);

            log.debug("[role-agent] {} ({}) turn {} complete: {} tool calls, {}ms",
                    name, role, turn, executions.size(), elapsed.toMillis());

            return new TurnResult(agentId, name, role, turn, input, finalResponse,
                    executions, true, null, elapsed);

        } catch (Exception e) {
            state.set(State.FAILED);
            log.error("[role-agent] {} ({}) turn failed: {}", name, role, e.getMessage());
            return TurnResult.failed(agentId, name, role, input, e.getMessage(),
                    Duration.between(start, Instant.now()));
        }
    }

    /**
     * Registers a tool stub for offline testing.
     */
    public RoleAgent stubTool(String toolName,
                               java.util.function.Function<Map<String,String>, String> fn) {
        toolStubs.put(toolName, fn);
        return this;
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    public String         agentId()    { return agentId; }
    public String         name()       { return name; }
    public AgentRole      role()       { return role; }
    public State          state()      { return state.get(); }
    public int            turnCount()  { return turnCount.get(); }
    public List<TurnRecord> turns()    { return List.copyOf(turns); }

    /** Returns the last response produced by this agent, or empty. */
    public Optional<String> lastResponse() {
        if (turns.isEmpty()) return Optional.empty();
        return Optional.of(turns.get(turns.size()-1).finalResponse());
    }

    /** True if the agent has produced output and is not in an error state. */
    public boolean hasOutput() {
        return !turns.isEmpty() && state.get() != State.FAILED;
    }

    public void reset() {
        history.clear();
        turns.clear();
        turnCount.set(0);
        state.set(State.IDLE);
    }

    // ── Private ────────────────────────────────────────────────────────────

    private String buildSystemPrompt(String task) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are ").append(name).append(", an AI agent.\n\n");
        sb.append(role.toSystemPromptBlock()).append("\n");

        // Tool protocol
        sb.append("## Tool Protocol\n");
        sb.append("Call tools using XML blocks. Permitted tools for your role: ");
        sb.append(String.join(", ", role.allowedTools())).append("\n");
        sb.append("```xml\n<tool_call><n>tool_name</n><param>value</param></tool_call>\n```\n\n");

        // Task context
        sb.append("## Current Task\n").append(task).append("\n");
        return sb.toString();
    }

    private String callLlm(String systemPrompt) throws SdkException {
        List<Message> messages = new ArrayList<>(history);
        InferenceRequest req = InferenceRequest.builder()
                .model(model)
                .systemPrompt(systemPrompt)
                .messages(messages)
                .temperature(config != null ? config.temperature() : 0.7)
                .maxTokens(config != null ? config.maxTokens() : 2048)
                .streaming(false)
                .build();
        InferenceResponse resp = sdk.createCompletion(req);
        return resp.getContent() != null ? resp.getContent() : "";
    }

    private List<ParsedToolCall> parseToolCalls(String text) {
        if (!text.contains("<tool_call")) return List.of();
        List<ParsedToolCall> calls = new ArrayList<>();
        int pos = 0;
        while (pos < text.length()) {
            int s = text.indexOf("<tool_call", pos);
            if (s < 0) break;
            int e = text.indexOf("</tool_call>", s);
            if (e < 0) break;
            String block = text.substring(s, e + 12);
            String name = extractTag(block, "n");
            if (name == null) name = extractTag(block, "name");
            if (name != null && !name.isBlank()) {
                Map<String, String> params = extractAllParams(block);
                params.remove("n"); params.remove("name");
                calls.add(new ParsedToolCall(name.strip(), params, block));
            }
            pos = e + 12;
        }
        return calls;
    }

    private ToolExecution executeToolCall(ParsedToolCall call) {
        Instant start = Instant.now();

        // Enforce role tool allowlist
        if (!role.allowedTools().contains(call.name())) {
            return ToolExecution.forbidden(call.name(), call.params(),
                    "Role " + role + " is not allowed to use tool '" + call.name() +
                    "'. Allowed: " + role.allowedTools(),
                    Duration.between(start, Instant.now()));
        }

        // Use stub if registered (for testing)
        if (toolStubs.containsKey(call.name())) {
            String output = toolStubs.get(call.name()).apply(call.params());
            return ToolExecution.success(call.name(), call.params(), output,
                    Duration.between(start, Instant.now()));
        }

        // In production, dispatch to real ToolExecutor via CDI
        // Here we return a placeholder for the base case
        return ToolExecution.success(call.name(), call.params(),
                "[Tool output for " + call.name() + " — inject ToolExecutor for real execution]",
                Duration.between(start, Instant.now()));
    }

    private void publishFindings(String response, List<ToolExecution> executions) {
        if (bus == null || response.isBlank()) return;
        bus.blackboard().post(
                agentId + ":latest", agentId,
                response.length() > 500 ? response.substring(0, 500) + "…" : response,
                0.8);
        executions.stream()
                .filter(ToolExecution::success)
                .forEach(ex -> bus.blackboard().post(
                        agentId + ":tool:" + ex.toolName(), agentId,
                        ex.output().length() > 300 ? ex.output().substring(0,300)+"…" : ex.output(),
                        0.7));
    }

    private String extractTag(String xml, String tag) {
        String open = "<" + tag + ">", close = "</" + tag + ">";
        int s = xml.indexOf(open);
        if (s < 0) return null;
        int e = xml.indexOf(close, s);
        if (e < 0) return null;
        return xml.substring(s + open.length(), e).strip();
    }

    private Map<String, String> extractAllParams(String xml) {
        Map<String, String> out = new LinkedHashMap<>();
        int pos = 0;
        while (pos < xml.length()) {
            int open = xml.indexOf('<', pos);
            if (open < 0) break;
            if (open + 1 < xml.length() && xml.charAt(open + 1) == '/') { pos = open + 2; continue; }
            int nameEnd = xml.indexOf('>', open + 1);
            if (nameEnd < 0) break;
            String tag = xml.substring(open + 1, nameEnd).strip();
            if (tag.isEmpty() || tag.contains(" ") || tag.equals("tool_call")) { pos = nameEnd + 1; continue; }
            String close = "</" + tag + ">";
            int closePos = xml.indexOf(close, nameEnd);
            if (closePos < 0) { pos = nameEnd + 1; continue; }
            out.put(tag, xml.substring(nameEnd + 1, closePos).strip());
            pos = closePos + close.length();
        }
        return out;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum State { IDLE, THINKING, ACTING, WAITING, DONE, FAILED }

    public record ParsedToolCall(String name, Map<String,String> params, String rawXml) {}

    public record ToolExecution(
            String              toolName,
            Map<String,String>  params,
            String              output,
            boolean             success,
            boolean             forbidden,
            String              error,
            Duration            elapsed
    ) {
        static ToolExecution success(String t, Map<String,String> p, String o, Duration d) {
            return new ToolExecution(t, p, o, true, false, null, d);
        }
        static ToolExecution forbidden(String t, Map<String,String> p, String e, Duration d) {
            return new ToolExecution(t, p, "", false, true, e, d);
        }
        static ToolExecution failed(String t, Map<String,String> p, String e, Duration d) {
            return new ToolExecution(t, p, "", false, false, e, d);
        }
        String toXml() {
            if (success) return "<tool_result name=\"" + toolName + "\">\n" + output + "\n</tool_result>";
            return "<tool_result name=\"" + toolName + "\" status=\"error\">\n" + error + "\n</tool_result>";
        }
    }

    public record TurnRecord(
            int                  turnNumber,
            String               input,
            String               rawResponse,
            String               finalResponse,
            List<ToolExecution>  toolExecutions,
            Duration             elapsed,
            Instant              completedAt
    ) {}

    public record TurnResult(
            String               agentId,
            String               agentName,
            AgentRole            role,
            int                  turnNumber,
            String               input,
            String               response,
            List<ToolExecution>  toolExecutions,
            boolean              success,
            String               error,
            Duration             elapsed
    ) {
        static TurnResult maxTurnsReached(String id, AgentRole r, int n) {
            return new TurnResult(id, id, r, n, "", "", List.of(), false,
                    "Max turns (" + n + ") reached", Duration.ZERO);
        }
        static TurnResult failed(String id, String name, AgentRole r,
                                  String input, String error, Duration d) {
            return new TurnResult(id, name, r, 0, input, "", List.of(), false, error, d);
        }
        public boolean hasToolCalls() { return !toolExecutions.isEmpty(); }
    }

    // ── Builder ────────────────────────────────────────────────────────────

    public static final class Builder {
        private final String    agentId;
        private final AgentRole role;
        private String          name;
        private GollekSdk       sdk;
        private GamelanConfig   config;
        private AgentMessageBus bus;
        private String          model;

        Builder(String agentId, AgentRole role) {
            this.agentId = agentId;
            this.role    = role;
            this.name    = role.name().toLowerCase().replace("_","-") + "-" + agentId.substring(0, Math.min(4, agentId.length()));
        }

        public Builder name(String n)       { this.name   = n;   return this; }
        public Builder sdk(GollekSdk s)     { this.sdk    = s;   return this; }
        public Builder config(GamelanConfig c) { this.config = c; return this; }
        public Builder bus(AgentMessageBus b)  { this.bus   = b;  return this; }
        public Builder model(String m)      { this.model  = m;   return this; }
        public RoleAgent build()            { return new RoleAgent(this); }
    }
}

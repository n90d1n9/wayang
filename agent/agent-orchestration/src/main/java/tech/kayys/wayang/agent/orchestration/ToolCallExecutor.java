package tech.kayys.wayang.agent.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import tech.kayys.wayang.agent.memory.AgentMemory;
import tech.kayys.wayang.agent.memory.AgentMemory;
import tech.kayys.wayang.agent.spi.DefaultSkillRegistry;
import tech.kayys.wayang.agent.spi.SkillContext;
import tech.kayys.wayang.agent.spi.SkillResult;
import tech.kayys.wayang.agent.spi.*;
import tech.kayys.gollek.spi.context.RequestContext;
import tech.kayys.gollek.tools.spi.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Parses tool/function calls from raw model output and dispatches them
 * to the appropriate registered {@link AgentSkill}.
 *
 * <h2>Two parsing modes</h2>
 * <ol>
 *   <li><b>Native tool call</b> — the model's inference response carries a
 *       structured {@code tool_calls} JSON array (OpenAI format) or
 *       {@code tool_use} content blocks (Anthropic format). No text parsing
 *       needed; we deserialise directly.</li>
 *   <li><b>Text-mode fallback</b> — models that don't support native function
 *       calling emit text like {@code Action: get_weather} /
 *       {@code Action Input: {"city":"Paris"}}. We parse that with a regex
 *       chain (legacy ReAct format).</li>
 * </ol>
 *
 * <h2>Parallel execution</h2>
 * When the model emits <em>multiple</em> tool calls in a single turn (allowed
 * by OpenAI and Anthropic), we execute them concurrently using
 * {@code Uni.join().all()} and return all results at once.
 */
@ApplicationScoped
public class ToolCallExecutor {

    private static final Logger LOG = Logger.getLogger(ToolCallExecutor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject DefaultSkillRegistry skillRegistry;
    @Inject AgentMemory memory;
    @Inject ToolMetrics metrics;

    // ── Parse: native OpenAI tool_calls array ─────────────────────────────────

    /**
     * Parse a JSON string that may contain an OpenAI-style
     * {@code tool_calls} array at the top level or inside a {@code message} wrapper.
     *
     * @param toolCallsJson the raw JSON from the model response
     * @return list of parsed ToolCall objects (empty if none found)
     */
    public List<ToolCall> parseOpenAIToolCalls(String toolCallsJson) {
        if (toolCallsJson == null || toolCallsJson.isBlank()) return List.of();
        try {
            JsonNode root = MAPPER.readTree(toolCallsJson);
            // Accept both array-at-root and message.tool_calls
            JsonNode arr = root.isArray() ? root
                    : root.path("tool_calls").isArray() ? root.path("tool_calls")
                    : root.path("message").path("tool_calls");

            if (!arr.isArray()) return List.of();
            List<ToolCall> calls = new ArrayList<>();
            for (JsonNode n : arr) calls.add(ToolCall.fromOpenAI(n));
            return Collections.unmodifiableList(calls);
        } catch (Exception e) {
            LOG.warnf("Failed to parse OpenAI tool_calls JSON: %s", e.getMessage());
            return List.of();
        }
    }

    /**
     * Parse Anthropic-style {@code tool_use} content blocks from a content array.
     */
    public List<ToolCall> parseAnthropicToolUse(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) return List.of();
        try {
            JsonNode arr = MAPPER.readTree(contentJson);
            if (!arr.isArray()) return List.of();
            List<ToolCall> calls = new ArrayList<>();
            for (JsonNode n : arr) {
                if ("tool_use".equals(n.path("type").asText()))
                    calls.add(ToolCall.fromAnthropic(n));
            }
            return Collections.unmodifiableList(calls);
        } catch (Exception e) {
            LOG.warnf("Failed to parse Anthropic tool_use JSON: %s", e.getMessage());
            return List.of();
        }
    }

    /**
     * Parse text-mode ReAct format:
     * <pre>
     * Action: skill_name
     * Action Input: {"key": "value"}
     * </pre>
     * Returns at most one ToolCall (text mode doesn't support parallel calls).
     */
    public Optional<ToolCall> parseTextModeAction(String llmOutput) {
        if (llmOutput == null) return Optional.empty();
        // Find "Action:" line
        java.util.regex.Matcher actionM = java.util.regex.Pattern
                .compile("Action:\\s*([^\\n]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(llmOutput);
        if (!actionM.find()) return Optional.empty();
        String skillName = actionM.group(1).strip();

        // Find "Action Input:" block (JSON object or value)
        java.util.regex.Matcher inputM = java.util.regex.Pattern
                .compile("Action Input:\\s*(\\{[\\s\\S]+?\\}|[^\\n]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(llmOutput);
        String rawInput = "{}";
        if (inputM.find()) rawInput = inputM.group(1).strip();

        // If it's not JSON, wrap in {"input": "..."}
        if (!rawInput.startsWith("{")) {
            rawInput = "{\"input\":\"" + rawInput.replace("\"", "\\\"") + "\"}";
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> args = MAPPER.readValue(rawInput, LinkedHashMap.class);
            return Optional.of(ToolCall.of(skillName, args));
        } catch (Exception e) {
            // Still create a call with the raw string as "input"
            return Optional.of(ToolCall.of(skillName, Map.of("input", rawInput)));
        }
    }

    // ── Execute ────────────────────────────────────────────────────────────────

    /**
     * Execute a single tool call and return its result.
     *
     * @param call    the parsed tool call
     * @param request the originating agent request (for tenant/model context)
     * @param runId   the current agent run id
     * @param step    the current reasoning step number
     * @return reactive result
     */
    public Uni<ToolCallResult> execute(ToolCall call, AgentRequest request, String runId, int step) {
        long t0 = System.currentTimeMillis();
        LOG.infof("Executing tool call: id=%s name=%s step=%d runId=%s",
                call.id(), call.name(), step, runId);

        Optional<AgentSkill> skillOpt = skillRegistry.find(call.name());
        if (skillOpt.isEmpty()) {
            String msg = "Unknown skill '" + call.name() + "'. Available: "
                    + skillRegistry.listAll().stream().map(AgentSkill::id).collect(Collectors.joining(", "));
            LOG.warnf(msg);
            metrics.record(call.name(), t0, true);
            return Uni.createFrom().item(ToolCallResult.error(call.id(), call.name(), msg));
        }

        AgentSkill skill = skillOpt.get();
        return memory.getAllWorking(runId)
                .chain(workingMemory -> {
                    SkillContext ctx = buildContext(call, request, runId, step, workingMemory);
                    return skill.execute(ctx)
                            .map(result -> {
                                metrics.record(call.name(), t0, !result.isSuccess());
                                if (result.isSuccess()) {
                                    // Persist any memory updates the skill declared
                                    result.getMemoryUpdates().forEach((k, v) ->
                                            memory.setWorking(runId, k, v).subscribe().with(x -> {}, e ->
                                                    LOG.warnf("Working memory write failed for key=%s: %s", k, e.getMessage())));
                                    return ToolCallResult.ok(call.id(), call.name(),
                                            result.getObservation(), Map.of());
                                } else {
                                    return ToolCallResult.error(call.id(), call.name(),
                                            result.getObservation());
                                }
                            });
                })
                .onFailure().recoverWithUni(err -> {
                    LOG.errorf(err, "Tool execution threw for skill=%s callId=%s", call.name(), call.id());
                    metrics.record(call.name(), t0, true);
                    return Uni.createFrom().item(ToolCallResult.error(call.id(), call.name(), err.getMessage()));
                });
    }

    /**
     * Execute multiple tool calls concurrently (parallel tool use).
     *
     * @param calls   list of tool calls from a single model turn
     * @param request originating request
     * @param runId   run id
     * @param step    step number
     * @return list of results in the same order as {@code calls}
     */
    public Uni<List<ToolCallResult>> executeAll(List<ToolCall> calls, AgentRequest request,
                                                 String runId, int step) {
        if (calls.isEmpty()) return Uni.createFrom().item(List.of());
        if (calls.size() == 1) {
            return execute(calls.get(0), request, runId, step).map(List::of);
        }
        // Parallel execution — all failures are caught per-call; the list is always complete
        List<Uni<ToolCallResult>> unis = calls.stream()
                .map(c -> execute(c, request, runId, step))
                .collect(Collectors.toList());
        return Uni.join().all(unis).andCollectFailures()
                .onFailure().recoverWithItem(err -> {
                    LOG.errorf(err, "Parallel tool execution partly failed");
                    return List.of(ToolCallResult.error("parallel-error", "batch", err.getMessage()));
                });
    }

    // ── SkillContext construction ──────────────────────────────────────────────

    private SkillContext buildContext(ToolCall call, AgentRequest request, String runId, int step, Map<String, Object> workingMemory) {
        return SkillContext.builder()
                .skillId(call.name())
                .invocationId(call.id())
                .runId(runId)
                .stepNumber(step)
                .inputs(call.arguments())
                .tenantId(request.tenantId() != null ? request.tenantId() : "community")
                .workingMemory(workingMemory)
                .timeout(Duration.ofSeconds(30))
                .build();
    }
}

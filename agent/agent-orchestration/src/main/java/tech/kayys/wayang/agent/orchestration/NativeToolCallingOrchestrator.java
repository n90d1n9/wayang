package tech.kayys.wayang.agent.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import tech.kayys.wayang.agent.memory.AgentMemory;
import tech.kayys.wayang.agent.spi.DefaultSkillRegistry;
import tech.kayys.wayang.agent.spi.*;
import tech.kayys.gollek.engine.inference.InferenceService;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.gollek.spi.tool.ToolDefinition;
import tech.kayys.gollek.tools.spi.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.HashSet;

/**
 * Native tool-calling ReAct orchestrator.
 */
@ApplicationScoped
public class NativeToolCallingOrchestrator implements AgentOrchestrator {

    private static final Logger LOG = Logger.getLogger(NativeToolCallingOrchestrator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject InferenceService     inferenceService;
    @Inject DefaultSkillRegistry skillRegistry;
    @Inject ToolCallExecutor     toolCallExecutor;
    @Inject AgentMemory          memory;
    @Inject ToolMetrics          metrics;

    @Override public String strategyId() { return "react"; }  // replaces old ReActOrchestrator

    // ── Main execute ──────────────────────────────────────────────────────────

    @Override
    public Uni<AgentResponse> execute(AgentRequest request) {
        String runId   = UUID.randomUUID().toString();
        Instant start  = Instant.now();

        // Build initial conversation
        List<Message> history = buildInitialHistory(request);
        List<ToolDefinition> tools = buildToolDefinitions(request);

        return loop(request, runId, history, tools, 0)
                .map(result -> AgentResponse.builder()
                        .runId(runId)
                        .requestId(request.requestId())
                        .answer(result.answer())
                        .totalSteps(result.steps())
                        .successful(true)
                        .strategy(strategyId())
                        .durationMs(Duration.between(start, Instant.now()).toMillis())
                        .build())
                .onFailure().recoverWithItem(err -> {
                    LOG.errorf(err, "ReAct run %s failed", runId);
                    return AgentResponse.builder()
                            .runId(runId).requestId(request.requestId())
                            .answer("").successful(false).error(err.getMessage())
                            .strategy(strategyId())
                            .durationMs(Duration.between(start, Instant.now()).toMillis())
                            .build();
                })
                .invoke(() -> metrics.finish())
                .invoke(() -> persistHistory(request, runId, history));
    }

    @Override
    public Multi<AgentEvent> stream(AgentRequest request) {
        return Multi.createFrom().emitter(emitter -> {
            String runId = UUID.randomUUID().toString();
            emitter.emit(AgentEvent.runStart(runId, strategyId()));

            List<Message> history = buildInitialHistory(request);
            List<ToolDefinition> tools = buildToolDefinitions(request);
            Instant start = Instant.now();

            streamLoop(request, runId, history, tools, 0, emitter)
                    .subscribe().with(
                            result -> {
                                emitter.emit(AgentEvent.finalAnswer(runId, result.steps(), result.answer()));
                                emitter.emit(AgentEvent.runComplete(runId, result.steps(), true,
                                        Duration.between(start, Instant.now()).toMillis()));
                                emitter.complete();
                            },
                            err -> {
                                emitter.emit(AgentEvent.error(runId, err.getMessage()));
                                emitter.complete();
                            });
        });
    }

    @Override
    public Uni<AgentState> step(AgentState state) {
        // Delegate to execute for single-step; streaming variant preferred for step mode
        return Uni.createFrom().failure(new UnsupportedOperationException(
                "Use execute() or stream() for NativeToolCallingOrchestrator"));
    }

    @Override
    public boolean isTerminal(AgentState state) {
        return state.getPhase() == AgentState.Phase.COMPLETE || state.getPhase() == AgentState.Phase.FAILED;
    }

    @Override
    public String getSystemPromptFragment() {
        return """
                You are a precise, helpful AI assistant with access to tools.
                When you need information or need to perform an action, use the provided tools.
                Think step by step. After gathering necessary information, provide a clear final answer.
                Always prefer using tools when they can provide accurate, real-time information.
                """;
    }

    // ── Recursive reasoning loop ───────────────────────────────────────────────

    private Uni<LoopResult> loop(AgentRequest request, String runId,
                                  List<Message> history, List<ToolDefinition> tools, int step) {
        int maxSteps = request.getMaxSteps();
        if (step >= maxSteps) {
            LOG.warnf("ReAct %s: max steps (%d) reached — returning best available answer", runId, maxSteps);
            String lastAssistant = findLastAssistantContent(history);
            return Uni.createFrom().item(new LoopResult(
                    lastAssistant.isBlank() ? "Max steps reached without a definitive answer." : lastAssistant, step));
        }

        return callModel(request, runId, history, tools, step)
                .chain(response -> {
                    // 1. Check for native tool calls in the response
                    List<ToolCall> nativeCalls = extractNativeToolCalls(response);
                    if (!nativeCalls.isEmpty()) {
                        // Append the assistant's tool-call turn
                        appendAssistantToolCallTurn(history, response, nativeCalls);
                        // Execute all tool calls (possibly parallel)
                        return toolCallExecutor.executeAll(nativeCalls, request, runId, step)
                                .chain(results -> {
                                    appendToolResults(history, results);
                                    return loop(request, runId, history, tools, step + 1);
                                });
                    }

                    // 2. Check for Final Answer in text
                    String text = response.getContent();
                    if (text != null) {
                        String finalAnswer = extractFinalAnswer(text);
                        if (finalAnswer != null) {
                            history.add(Message.assistant(text));
                            return Uni.createFrom().item(new LoopResult(finalAnswer, step + 1));
                        }

                        // 3. Text-mode fallback: parse Action/Action Input
                        Optional<ToolCall> textCall = toolCallExecutor.parseTextModeAction(text);
                        if (textCall.isPresent()) {
                            history.add(Message.assistant(text));
                            return toolCallExecutor.execute(textCall.get(), request, runId, step)
                                    .chain(result -> {
                                        history.add(Message.tool(textCall.get().id(), result.content()));
                                        history.add(Message.user("Observation: " + result.content()));
                                        return loop(request, runId, history, tools, step + 1);
                                    });
                        }

                        // 4. No action found — return text as answer
                        history.add(Message.assistant(text));
                        return Uni.createFrom().item(new LoopResult(text.strip(), step + 1));
                    }

                    return Uni.createFrom().item(new LoopResult("No response generated.", step));
                });
    }

    private Uni<LoopResult> streamLoop(AgentRequest request, String runId,
                                        List<Message> history, List<ToolDefinition> tools,
                                        int step, io.smallrye.mutiny.subscription.MultiEmitter<? super AgentEvent> emitter) {
        int maxSteps = request.getMaxSteps();
        if (step >= maxSteps) {
            return Uni.createFrom().item(new LoopResult(findLastAssistantContent(history), step));
        }

        return callModel(request, runId, history, tools, step)
                .chain(response -> {
                    List<ToolCall> nativeCalls = extractNativeToolCalls(response);
                    String text = response.getContent();

                    if (!nativeCalls.isEmpty()) {
                        nativeCalls.forEach(c ->
                                emitter.emit(AgentEvent.action(runId, c.name() + "(" + c.arguments() + ")")));
                        appendAssistantToolCallTurn(history, response, nativeCalls);
                        return toolCallExecutor.executeAll(nativeCalls, request, runId, step)
                                .chain(results -> {
                                    results.forEach(r ->
                                            emitter.emit(AgentEvent.observation(runId, r.content())));
                                    appendToolResults(history, results);
                                    return streamLoop(request, runId, history, tools, step + 1, emitter);
                                });
                    }

                    if (text != null) {
                        String thought = extractThought(text);
                        if (thought != null) emitter.emit(AgentEvent.thought(runId, thought));

                        String finalAnswer = extractFinalAnswer(text);
                        if (finalAnswer != null) {
                            history.add(Message.assistant(text));
                            return Uni.createFrom().item(new LoopResult(finalAnswer, step + 1));
                        }

                        Optional<ToolCall> textCall = toolCallExecutor.parseTextModeAction(text);
                        if (textCall.isPresent()) {
                            emitter.emit(AgentEvent.action(runId, textCall.get().name() + "(" + textCall.get().arguments() + ")"));
                            history.add(Message.assistant(text));
                            return toolCallExecutor.execute(textCall.get(), request, runId, step)
                                    .chain(result -> {
                                        emitter.emit(AgentEvent.observation(runId, result.content()));
                                        history.add(Message.tool(textCall.get().id(), result.content()));
                                        history.add(Message.user("Observation: " + result.content()));
                                        return streamLoop(request, runId, history, tools, step + 1, emitter);
                                    });
                        }
                        history.add(Message.assistant(text));
                        return Uni.createFrom().item(new LoopResult(text.strip(), step + 1));
                    }
                    return Uni.createFrom().item(new LoopResult("No response.", step));
                });
    }

    // ── Model call ────────────────────────────────────────────────────────────

    private Uni<InferenceResponse> callModel(AgentRequest request, String runId,
                                              List<Message> history, List<ToolDefinition> tools, int step) {
        InferenceRequest.Builder builder = InferenceRequest.builder()
                .requestId(runId + "-step-" + step)
                .model(request.modelId() != null ? request.modelId() : "default")
                .messages(history)
                .parameter("temperature", 0.7)
                .parameter("max_tokens", 1024)
                .metadata("tenantId", request.tenantId() != null ? request.tenantId() : "community");

        if (!tools.isEmpty()) {
            builder.tools(tools);
        }

        return inferenceService.inferAsync(builder.build());
    }

    // ── Tool schema building ───────────────────────────────────────────────────

    /**
     * Convert all registered (and allowed) skills to OpenAI-format ToolDefinition objects.
     * These are sent with the inference request to enable native function calling.
     */
    private List<ToolDefinition> buildToolDefinitions(AgentRequest request) {
        String tenantId = request.tenantId() != null ? request.tenantId() : "community";
        List<AgentSkill> skills = request.allowedSkills() != null && !request.allowedSkills().isEmpty()
                ? skillRegistry.listAllowed(tenantId, new HashSet<>(request.allowedSkills()))
                : skillRegistry.listAll();

        return skills.stream()
                .filter(AgentSkill::isHealthy)
                .map(this::skillToToolDefinition)
                .collect(Collectors.toList());
    }

    private ToolDefinition skillToToolDefinition(AgentSkill skill) {
        // Build from ToolSchema if the skill provides one
        ToolSchema schema = buildSchemaFor(skill);
        ObjectNode openAI = schema.toOpenAIFormat();

        return ToolDefinition.builder()
                .name(skill.id())
                .type(ToolDefinition.Type.FUNCTION)
                .description(skill.description())
                .parameters(Map.of(
                        "type", "object",
                        "properties", openAI.path("function").path("parameters").path("properties"),
                        "required",   openAI.path("function").path("parameters").path("required")
                ))
                .build();
    }

    /**
     * Build a ToolSchema from the skill's annotation or reasonable defaults.
     * Skills that override {@code toolSchema()} take precedence.
     */
    private ToolSchema buildSchemaFor(AgentSkill skill) {
        // If skill implements ToolSchemaProvider, use it
        if (skill instanceof ToolSchemaProvider tsp) {
            return tsp.toolSchema();
        }
        // Derive minimal schema from annotation
        var ann = skill.getClass().getAnnotation(tech.kayys.wayang.agent.spi.SkillDescriptor.class);
        ToolSchema.Builder b = ToolSchema.builder(skill.id()).description(skill.description());
        if (ann != null && ann.triggers().length > 0) {
            b.param("input", "string",
                    "Input for this tool. Related tasks: " + String.join(", ", ann.triggers()));
        } else {
            b.param("input", "string", "Input for " + skill.name());
        }
        return b.build();
    }

    /** Marker interface for skills that can provide their own detailed tool schema. */
    public interface ToolSchemaProvider {
        ToolSchema toolSchema();
    }

    // ── History helpers ────────────────────────────────────────────────────────

    private List<Message> buildInitialHistory(AgentRequest request) {
        List<Message> history = new ArrayList<>();
        String systemPrompt = request.systemPrompt() != null
                ? request.systemPrompt()
                : getSystemPromptFragment();
        history.add(Message.system(systemPrompt));

        // Inject prior conversation history from memory if session is provided
        if (request.sessionId() != null) {
            try {
                List<Message> prior = memory
                        .getConversation(request.sessionId(), request.tenantId() != null ? request.tenantId() : "community", 20)
                        .await().atMost(Duration.ofSeconds(3));
                history.addAll(prior);
            } catch (Exception e) {
                LOG.warnf("Could not load session history for %s: %s", request.sessionId(), e.getMessage());
            }
        }

        history.add(Message.user(request.prompt()));
        return history;
    }

    private void appendAssistantToolCallTurn(List<Message> history, InferenceResponse response, List<ToolCall> calls) {
        // Add the assistant's turn that included tool calls
        // We preserve the text content (if any) + mark it as a tool-calling turn
        String text = response.getContent() != null ? response.getContent() : "";
        history.add(Message.assistant(text));
    }

    private void appendToolResults(List<Message> history, List<ToolCallResult> results) {
        for (ToolCallResult r : results) {
            history.add(Message.tool(r.toolCallId(), r.content()));
        }
    }

    private void persistHistory(AgentRequest request, String runId, List<Message> history) {
        if (request.sessionId() == null) return;
        // Persist only user + assistant turns (skip system and tool messages for compactness)
        history.stream()
                .filter(m -> m.getRole() == Message.Role.USER || m.getRole() == Message.Role.ASSISTANT)
                .forEach(m -> memory.addMessage(request.sessionId(),
                                request.tenantId() != null ? request.tenantId() : "community", m)
                        .subscribe().with(v -> {}, e -> {}));
    }

    // ── Parse helpers ──────────────────────────────────────────────────────────

    /**
     * Extract tool calls from the model response.
     * Handles both OpenAI {@code tool_calls} metadata and Anthropic content blocks.
     */
    private List<ToolCall> extractNativeToolCalls(InferenceResponse response) {
        // Check response metadata for tool_calls JSON (how our InferenceResponse carries them)
        Map<String, Object> meta = response.getMetadata();
        if (meta == null) return List.of();

        Object toolCallsObj = meta.get("tool_calls");
        if (toolCallsObj instanceof String json && !json.isBlank()) {
            return toolCallExecutor.parseOpenAIToolCalls(json);
        }
        if (toolCallsObj instanceof List<?> list && !list.isEmpty()) {
            try {
                String json = MAPPER.writeValueAsString(list);
                return toolCallExecutor.parseOpenAIToolCalls(json);
            } catch (Exception e) {
                LOG.warnf("Could not serialise tool_calls list: %s", e.getMessage());
            }
        }

        // Check content for Anthropic tool_use blocks
        String content = response.getContent();
        if (content != null && content.contains("\"type\":\"tool_use\"")) {
            return toolCallExecutor.parseAnthropicToolUse(content);
        }
        return List.of();
    }

    private String extractFinalAnswer(String text) {
        if (text == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("Final Answer:\\s*(.+)", java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(text);
        return m.find() ? m.group(1).strip() : null;
    }

    private String extractThought(String text) {
        if (text == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("Thought:\\s*(.+?)(?=Action:|Final Answer:|$)", java.util.regex.Pattern.DOTALL)
                .matcher(text);
        return m.find() ? m.group(1).strip() : null;
    }

    private String findLastAssistantContent(List<Message> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            Message m = history.get(i);
            if (m.getRole() == Message.Role.ASSISTANT && m.getContent() != null && !m.getContent().isBlank())
                return m.getContent();
        }
        return "";
    }

    private record LoopResult(String answer, int steps) {}
}

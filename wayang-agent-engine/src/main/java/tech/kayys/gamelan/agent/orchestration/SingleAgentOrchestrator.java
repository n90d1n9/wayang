package tech.kayys.gamelan.agent.orchestration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.*;
import tech.kayys.gamelan.governance.AuditLog;
import tech.kayys.gamelan.memory.MemoryHierarchy;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.AgentMemory;
import tech.kayys.gamelan.skill.Skill;
import tech.kayys.gamelan.skill.SkillRegistry;
import tech.kayys.gamelan.skill.SkillSelector;
import tech.kayys.gamelan.tool.ToolExecutor;
import tech.kayys.gamelan.tool.ToolResult;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tier 2 — Single agent with tools (ReAct loop).
 *
 * <h2>Concurrency fix</h2>
 * Previous version had a shared {@code AtomicBoolean cancelled} on the
 * {@code @ApplicationScoped} singleton. Multiple parallel worker calls from
 * {@link MultiAgentOrchestrator} would corrupt each other's cancellation state
 * because they all share the same bean instance. Fixed: {@code cancelled} is
 * now a {@link ThreadLocal}, giving each calling thread its own flag.
 *
 * <h2>Tool allowlist enforcement</h2>
 * When {@link AgentRequest#allowedTools()} is non-empty, tool calls for names
 * not in the list are blocked with an error result — not silently dropped.
 * The LLM can see the error and adjust.
 *
 * <h2>Iteration guard</h2>
 * If the same tool call signature is produced 3+ consecutive times, the loop
 * breaks and appends a clear explanation so the LLM knows why it stopped.
 */
@ApplicationScoped
public class SingleAgentOrchestrator implements AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SingleAgentOrchestrator.class);
    private static final int LOOP_GUARD_THRESHOLD = 3;

    // ThreadLocal: each concurrent caller (parallel workers) gets its own flag
    private final ThreadLocal<AtomicBoolean> cancelled =
            ThreadLocal.withInitial(() -> new AtomicBoolean(false));

    @Inject GollekSdk      sdk;
    @Inject PromptBuilder  promptBuilder;
    @Inject ToolCallParser toolCallParser;
    @Inject ToolExecutor   toolExecutor;
    @Inject SkillSelector  skillSelector;
    @Inject SkillRegistry  skillRegistry;
    @Inject AgentMemory    memory;
    @Inject TokenTracker   tokenTracker;
    @Inject ToolMetrics     toolMetrics;
    @Inject AuditLog       auditLog;
    @Inject MemoryHierarchy memoryHierarchy;
    @Inject GamelanConfig  config;

    @Override public String strategyId()  { return "react"; }
    @Override public String displayName() { return "Single agent with tools (ReAct)"; }

    @Override
    public String description() {
        return "ReAct loop: plan → tool → observe → repeat. "
                + "Iteration-limited. Use for most coding tasks, file ops, debugging, refactoring.";
    }

    @Override
    public OrchestratorResult execute(AgentRequest request) {
        return execute(request, AgentEventListener.NOOP);
    }

    /**
     * Execute with event listener — used by the REPL to stream progress.
     */
    public OrchestratorResult execute(AgentRequest request, AgentEventListener listener) {
        Instant start = Instant.now();
        cancelled.get().set(false);

        String      model       = resolveModel(request);
        List<Skill> skills      = skillSelector.select(request.task(), skillRegistry.listAll());
        String      systemPrompt = buildSystemPrompt(request, skills);
        // Append task-specific deep memory (episodic + semantic vector retrieval)
        String deepMemory = memoryHierarchy.buildPromptBlock(request.task());
        if (!deepMemory.isBlank()) {
            systemPrompt = systemPrompt + "\n" + deepMemory;
        }

        List<ConversationMessage> messages = new ArrayList<>(request.session().toMessages());
        messages.add(ConversationMessage.user(request.task()));

        StringBuilder    fullText  = new StringBuilder();
        List<ToolResult> allTools  = new ArrayList<>();
        List<String>     intermediates = new ArrayList<>();
        boolean          hadError  = false;
        String           lastSig   = null;
        int              sameSigCount = 0;
        int              maxSteps  = request.maxSteps() > 0 ? request.maxSteps() : 10;

        auditLog.logRunStart(request.session().id(), request.task(), strategyId());
        listener.onRunStart(request.task(), model);

        for (int iter = 0; iter < maxSteps; iter++) {
            if (cancelled.get().get()) { log.info("[react] cancelled at iter {}", iter); break; }

            listener.onIterationStart(iter, maxSteps);
            String iterText = callLlm(model, systemPrompt, messages, request.stream(), listener);
            memory.extractAndStore(iterText);

            if (iterText.startsWith("[LLM_ERROR]")) {
                hadError = true;
                fullText.append(iterText);
                listener.onError(iterText, iter);
                break;
            }

            fullText.append(iterText);
            List<ToolCall> toolCalls = toolCallParser.parse(iterText);
            listener.onIterationEnd(iter, toolCalls.isEmpty() ? "stop" : "tool_calls");

            if (toolCalls.isEmpty()) break;

            // Loop guard: detect identical repeated calls
            String sig = toolCalls.stream()
                    .map(c -> c.name() + c.parameters()).reduce("", String::concat);
            if (sig.equals(lastSig)) {
                if (++sameSigCount >= LOOP_GUARD_THRESHOLD) {
                    String msg = "\n\n[Stopped: identical tool call repeated "
                            + sameSigCount + " times — possible loop]";
                    fullText.append(msg);
                    log.warn("[react] loop guard triggered at iter {}", iter);
                    break;
                }
            } else { lastSig = sig; sameSigCount = 1; }

            // Execute tool calls
            StringBuilder toolBlock = new StringBuilder();
            for (ToolCall call : toolCalls) {
                if (cancelled.get().get()) break;

                // Enforce tool allowlist
                if (request.hasToolFilter() && !request.allowedTools().contains(call.name())) {
                    String err = "Tool '" + call.name() + "' is not in the allowed list for this request.";
                    toolBlock.append(new ToolResult(call.name(), "", 1, err).toXml()).append("\n");
                    continue;
                }

                listener.onToolStart(call.name(), call.parameters().toString());
                long t0 = System.currentTimeMillis();
                ToolResult result = toolExecutor.execute(call);
                long elapsed = System.currentTimeMillis() - t0;

                toolMetrics.record(call.name(), t0, !result.isSuccess());
                allTools.add(result);
                toolBlock.append(result.toXml()).append("\n");
                listener.onToolEnd(call.name(), result.output(), !result.isSuccess(), elapsed);
            }

            messages.add(ConversationMessage.assistant(iterText));
            messages.add(ConversationMessage.user(toolBlock.toString()));
            intermediates.add(iterText);

            if (request.stream()) { System.out.println(); System.out.flush(); }
        }

        // Record token usage
        int sysLen = systemPrompt.length();
        int msgLen = messages.stream().mapToInt(m -> m.content().length()).sum();
        tokenTracker.record(sysLen, msgLen, fullText.length(), allTools.size());

        Duration elapsed = Duration.between(start, Instant.now());
        listener.onComplete(fullText.toString(), intermediates.size() + 1);

        // Record episode in memory hierarchy for learning
        long durMs = Duration.between(start, Instant.now()).toMillis();
        memoryHierarchy.recordEpisode(
                request.task(),
                hadError ? "Failed: " + fullText.toString().substring(0, Math.min(200, fullText.length())) : "Completed successfully",
                allTools.stream().map(t -> t.toolName()).distinct().toList(),
                !hadError, durMs);

        auditLog.logRunEnd(request.session().id(), !hadError, durMs);

        if (hadError) {
            return OrchestratorResult.failure(strategyId(), fullText.toString(), elapsed);
        }
        return OrchestratorResult.ok(fullText.toString(), strategyId(),
                intermediates.size() + 1, allTools, elapsed);
    }

    /** Cancel the current request on THIS thread. */
    public void cancelCurrentThread() { cancelled.get().set(true); }

    // ── Private ────────────────────────────────────────────────────────────

    private String buildSystemPrompt(AgentRequest request, List<Skill> skills) {
        String base = promptBuilder.buildSystemPrompt(skills);
        return request.systemExtra().isBlank()
                ? base
                : base + "\n\n## Additional Instructions\n" + request.systemExtra();
    }

    private String callLlm(String model, String system,
                             List<ConversationMessage> messages, boolean stream,
                             AgentEventListener listener) {
        InferenceRequest req = InferenceRequest.builder()
                .model(model)
                .message(Message.system(system))
                .messages(messages.stream()
                        .map(m -> new Message(Message.Role.valueOf(m.role().toUpperCase()), m.content()))
                        .toList())
                .temperature(config.temperature())
                .maxTokens(config.maxTokens())
                .streaming(stream)
                .build();

        if (stream) return streamAndCollect(req, listener);
        return blockAndCollect(req);
    }

    private String streamAndCollect(InferenceRequest req, AgentEventListener listener) {
        StringBuilder sb    = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> err =
                new java.util.concurrent.atomic.AtomicReference<>();

        sdk.streamCompletion(req).subscribe().with(
                chunk -> {
                    String d = chunk.delta();
                    if (d != null && !d.isEmpty()) {
                        System.out.print(d); System.out.flush();
                        sb.append(d);
                        listener.onTextChunk(d);
                    }
                },
                e -> { err.set(e); latch.countDown(); },
                () -> { System.out.println(); latch.countDown(); }
        );

        try {
            if (!latch.await(config.requestTimeoutSeconds(), TimeUnit.SECONDS)) {
                return "[LLM_ERROR] Stream timed out after " + config.requestTimeoutSeconds() + "s";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return sb.length() > 0 ? sb.toString() : "[LLM_ERROR] Interrupted";
        }
        return err.get() != null ? "[LLM_ERROR] " + err.get().getMessage() : sb.toString();
    }

    private String blockAndCollect(InferenceRequest req) {
        try {
            InferenceResponse r = sdk.createCompletion(req);
            return r.getContent() != null ? r.getContent() : "";
        } catch (SdkException e) { return "[LLM_ERROR] " + e.getMessage(); }
    }

    private String resolveModel(AgentRequest request) {
        return (request.model() != null && !request.model().isBlank())
                ? request.model() : config.defaultModel();
    }
}

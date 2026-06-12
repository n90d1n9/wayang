package tech.kayys.gamelan.agent.orchestration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.PromptBuilder;
import tech.kayys.gamelan.config.GamelanConfig;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tier 1 — Direct model call.
 *
 * <p>A single LLM call. No tool use. No agentic loop. No iteration.
 *
 * <h2>When to use</h2>
 * <ul>
 *   <li>Classification ("is this code safe?")</li>
 *   <li>Summarisation ("summarise this file in 3 bullets")</li>
 *   <li>Translation ("translate this Python to Java")</li>
 *   <li>Explanation ("explain what this regex does")</li>
 *   <li>Any task the model can solve in one pass from context alone</li>
 * </ul>
 *
 * <h2>Why not just always use the ReAct agent?</h2>
 * The direct tier is faster (one round-trip), cheaper (fewer tokens),
 * and easier to debug. If prompt engineering solves the problem,
 * you don't need an agent.
 *
 * <h2>How prompts differ</h2>
 * The system prompt for the direct tier is minimal: no tool catalogue,
 * no REMEMBER protocol, no tool protocol section. Just identity +
 * project context + the task.
 */
@ApplicationScoped
public class DirectCallOrchestrator implements AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DirectCallOrchestrator.class);

    @Inject GollekSdk     sdk;
    @Inject PromptBuilder promptBuilder;
    @Inject GamelanConfig config;

    @Override public String strategyId()  { return "direct"; }
    @Override public String displayName() { return "Direct model call"; }
    @Override public boolean supportsTools() { return false; }

    @Override
    public String description() {
        return "Single LLM call, no tools. Fast. Use for classification, "
                + "summarisation, translation, simple Q&A.";
    }

    @Override
    public OrchestratorResult execute(AgentRequest request) {
        Instant start  = Instant.now();
        String  model  = resolveModel(request);
        String  system = buildDirectSystemPrompt(request);

        // Build message list: existing session history + the new task
        List<Message> messages = new ArrayList<>();
        request.session().toMessages().forEach(m ->
                messages.add(Message.of(m.role(), m.content())));
        messages.add(Message.user(request.task()));

        log.info("[direct] model={} chars={}", model, request.task().length());

        try {
            String answer;
            if (request.stream()) {
                answer = streamCall(model, system, messages, request);
            } else {
                answer = blockCall(model, system, messages, request);
            }

            Duration elapsed = Duration.between(start, Instant.now());
            log.info("[direct] done in {}ms", elapsed.toMillis());
            return OrchestratorResult.ok(answer, strategyId(), 1, List.of(), elapsed);

        } catch (Exception e) {
            log.error("[direct] failed: {}", e.getMessage());
            return OrchestratorResult.failure(strategyId(), e.getMessage(),
                    Duration.between(start, Instant.now()));
        }
    }

    // ── Private ────────────────────────────────────────────────────────────

    /**
     * Minimal system prompt — no tool catalogue, no tool protocol, no REMEMBER.
     * Project context and any systemExtra from the request are included.
     */
    private String buildDirectSystemPrompt(AgentRequest request) {
        String base = promptBuilder.buildMinimalPrompt();
        if (!request.systemExtra().isBlank()) {
            base += "\n\n## Additional Instructions\n" + request.systemExtra();
        }
        return base;
    }

    private String blockCall(String model, String system,
                              List<Message> messages, AgentRequest request) throws SdkException {
        InferenceRequest req = InferenceRequest.builder()
                .model(model)
                .systemPrompt(system)
                .messages(messages)
                .temperature(config.temperature())
                .maxTokens(config.maxTokens())
                .streaming(false)
                .build();
        InferenceResponse resp = sdk.createCompletion(req);
        return resp.getContent() != null ? resp.getContent() : "";
    }

    private String streamCall(String model, String system,
                               List<Message> messages, AgentRequest request) {
        InferenceRequest req = InferenceRequest.builder()
                .model(model)
                .systemPrompt(system)
                .messages(messages)
                .temperature(config.temperature())
                .maxTokens(config.maxTokens())
                .streaming(true)
                .build();

        StringBuilder sb    = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();

        sdk.streamCompletion(req).subscribe().with(
                chunk -> { if (chunk.delta() != null) { System.out.print(chunk.delta()); System.out.flush(); sb.append(chunk.delta()); } },
                e -> { err.set(e); latch.countDown(); },
                () -> { System.out.println(); latch.countDown(); }
        );

        try { latch.await(config.requestTimeoutSeconds(), TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        if (err.get() != null) throw new RuntimeException(err.get());
        return sb.toString();
    }

    private String resolveModel(AgentRequest request) {
        return (request.model() != null && !request.model().isBlank())
                ? request.model() : config.defaultModel();
    }
}

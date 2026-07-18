package tech.kayys.gamelan.agent.orchestration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.PromptBuilder;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.AgentMemory;
import tech.kayys.gamelan.session.ConversationSession;
import tech.kayys.gamelan.tool.ToolResult;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Tier 3 — Multi-agent orchestration.
 *
 * <p>An <em>orchestrator agent</em> decomposes the task, assigns sub-tasks to
 * <em>specialised worker agents</em> running in parallel on virtual threads,
 * then synthesises their outputs into a final answer.
 *
 * <h2>Worker isolation</h2>
 * Each worker gets a brand-new {@link ConversationSession} constructed with
 * the correct {@code tokenBudget} from config — the previous version used
 * bare {@code new ConversationSession(null)} which ignored the budget setting.
 *
 * <h2>Decomposition robustness</h2>
 * The JSON parser is resilient to LLM formatting habits (markdown fences,
 * trailing commas, leading prose). If decomposition produces zero sub-tasks
 * the call falls back to single-agent rather than returning empty results.
 *
 * <h2>Synthesis quality</h2>
 * Only successful, non-empty worker outputs are included in the synthesis
 * prompt. Each worker's contribution is capped at 3 000 chars to prevent
 * context overflow. The synthesis step itself is a fresh LLM call (not a
 * tool-using agent) to avoid nested orchestration complexity.
 *
 * <h2>When to use over single agent</h2>
 * Cross-domain tasks, parallel specialisation, tasks that exceed a single
 * agent's reliable capability. Do NOT use for simple one-domain tasks —
 * the coordination overhead outweighs the benefit.
 */
@ApplicationScoped
public class MultiAgentOrchestrator implements AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentOrchestrator.class);

    /** Per-worker step limit — lower than single-agent to bound total cost. */
    private static final int WORKER_MAX_STEPS  = 5;
    /** Max chars per worker in the synthesis prompt. */
    private static final int WORKER_CONTRIB_CAP = 3_000;

    @Inject GollekSdk               sdk;
    @Inject SingleAgentOrchestrator singleAgent;
    @Inject PromptBuilder            promptBuilder;
    @Inject GamelanConfig            config;
    @Inject AgentMemory              memory;

    @Override public String strategyId()  { return "multi-agent"; }
    @Override public String displayName() { return "Multi-agent orchestration"; }

    @Override
    public String description() {
        return "Orchestrator decomposes task → parallel worker agents → synthesis. "
                + "Use for cross-domain, parallel-specialisation tasks only.";
    }

    @Override
    public OrchestratorResult execute(AgentRequest request) {
        Instant start = Instant.now();
        String  model = resolveModel(request);

        log.info("[multi-agent] model={}", model);

        try {
            // Step 1: Orchestrator decomposes the task
            List<SubTask> subTasks = decompose(model, request);
            if (subTasks.isEmpty()) {
                log.warn("[multi-agent] no sub-tasks from decomposition — falling back to single-agent");
                return singleAgent.execute(request);
            }
            log.info("[multi-agent] decomposed into {} sub-tasks: {}",
                    subTasks.size(), subTasks.stream().map(SubTask::name).toList());

            // Step 2: Run worker agents in parallel
            List<WorkerResult> workerResults = runWorkersParallel(subTasks, request, model);

            // Step 3: Synthesise
            String synthesis = synthesise(model, request, workerResults);

            List<ToolResult> allTools = workerResults.stream()
                    .flatMap(w -> w.toolResults().stream())
                    .toList();

            Duration elapsed = Duration.between(start, Instant.now());
            log.info("[multi-agent] done in {}ms, workers={} tools={}",
                    elapsed.toMillis(), workerResults.size(), allTools.size());

            return OrchestratorResult.ok(synthesis, strategyId(),
                    subTasks.size() + 1, allTools, elapsed);

        } catch (Exception e) {
            log.error("[multi-agent] failed: {}", e.getMessage(), e);
            return OrchestratorResult.failure(strategyId(), e.getMessage(),
                    Duration.between(start, Instant.now()));
        }
    }

    // ── Decomposition ──────────────────────────────────────────────────────

    private List<SubTask> decompose(String model, AgentRequest request) throws SdkException {
        String prompt = """
                Decompose the following task into 2-5 independent sub-tasks for parallel
                specialist agents. Each sub-task should focus on one domain.

                Reply ONLY with a valid JSON array (no prose, no markdown fences):
                [
                  {"name": "unique-id", "task": "Detailed instructions for this worker."},
                  ...
                ]

                TASK:
                %s
                """.formatted(request.task());

        InferenceResponse resp = sdk.createCompletion(InferenceRequest.builder()
                .model(model)
                .message(Message.system("You are a task decomposition assistant. Reply only in JSON."))
                .messages(List.of(Message.user(prompt)))
                .temperature(0.3)
                .maxTokens(1024)
                .streaming(false)
                .build());

        return parseSubTasks(resp.getContent());
    }

    /** Robust JSON parser — handles markdown fences, leading prose, trailing commas. */
    private List<SubTask> parseSubTasks(String raw) {
        if (raw == null || raw.isBlank()) return List.of();

        // Strip markdown fences
        String json = raw.replaceAll("(?s)```json\\s*", "")
                         .replaceAll("```", "")
                         .strip();

        // Find the JSON array bounds
        int arrStart = json.indexOf('[');
        int arrEnd   = json.lastIndexOf(']');
        if (arrStart < 0 || arrEnd <= arrStart) {
            log.warn("[multi-agent] no JSON array in decomposition response");
            return List.of();
        }
        json = json.substring(arrStart, arrEnd + 1);

        // Remove trailing commas before ] or } (common LLM mistake)
        json = json.replaceAll(",\\s*([}\\]])", "$1");

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode arr = mapper.readTree(json);
            List<SubTask> tasks = new ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode n : arr) {
                String name = n.path("name").asText("worker-" + tasks.size());
                String task = n.path("task").asText("").strip();
                if (!task.isBlank()) tasks.add(new SubTask(name, task));
            }
            return tasks;
        } catch (Exception e) {
            log.warn("[multi-agent] JSON parse error: {}", e.getMessage());
            return List.of();
        }
    }

    // ── Parallel worker execution ───────────────────────────────────────────

    private List<WorkerResult> runWorkersParallel(List<SubTask> subTasks,
                                                   AgentRequest request, String model) {
        long timeoutSecs = (long) config.requestTimeoutSeconds() * 2;

        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();

        // Submit all futures BEFORE calling exec.shutdown()
        Map<SubTask, Future<WorkerResult>> futures = new LinkedHashMap<>();
        for (SubTask sub : subTasks) {
            SubTask cap = sub;
            futures.put(cap, exec.submit(() -> {
                log.info("[worker] '{}' starting", cap.name());
                Instant ws = Instant.now();

                // Fresh session with correct config values (bug fix: was bare new ConversationSession(null))
                AgentRequest workerReq = AgentRequest.builder(cap.task())
                        .model(model)
                        .session(new ConversationSession(null,
                                config.sessionPersist(), config.tokenBudget()))
                        .stream(false)          // workers never interleave to stdout
                        .maxSteps(WORKER_MAX_STEPS)
                        .systemExtra(request.systemExtra())
                        .allowedTools(request.allowedTools())
                        .build();

                OrchestratorResult result = singleAgent.execute(workerReq);
                Duration elapsed = Duration.between(ws, Instant.now());
                log.info("[worker] '{}' done in {}ms success={}",
                        cap.name(), elapsed.toMillis(), result.success());

                return new WorkerResult(cap.name(), result.answer(),
                        result.success(), result.toolResults(), elapsed);
            }));
        }

        exec.shutdown(); // No new tasks; existing ones continue

        List<WorkerResult> results = new ArrayList<>();
        for (Map.Entry<SubTask, Future<WorkerResult>> entry : futures.entrySet()) {
            SubTask sub = entry.getKey();
            try {
                results.add(entry.getValue().get(timeoutSecs, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                entry.getValue().cancel(true);
                log.warn("[worker] '{}' timed out after {}s", sub.name(), timeoutSecs);
                results.add(new WorkerResult(sub.name(), "", false, List.of(), Duration.ZERO));
            } catch (ExecutionException e) {
                log.error("[worker] '{}' threw: {}", sub.name(), e.getCause().getMessage());
                results.add(new WorkerResult(sub.name(),
                        "Error: " + e.getCause().getMessage(),
                        false, List.of(), Duration.ZERO));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return results;
    }

    // ── Synthesis ──────────────────────────────────────────────────────────

    private String synthesise(String model, AgentRequest request,
                               List<WorkerResult> workers) throws SdkException {
        List<WorkerResult> successful = workers.stream()
                .filter(w -> w.success() && !w.output().isBlank())
                .toList();

        if (successful.isEmpty()) {
            return "All worker agents failed to produce output. "
                    + "Consider retrying with --strategy react.";
        }

        StringBuilder merged = new StringBuilder();
        for (WorkerResult w : successful) {
            merged.append("## Worker: ").append(w.name())
                  .append(" (").append(w.elapsed().getSeconds()).append("s)\n");
            String contrib = w.output().length() > WORKER_CONTRIB_CAP
                    ? w.output().substring(0, WORKER_CONTRIB_CAP) + "\n…(truncated)"
                    : w.output();
            merged.append(contrib).append("\n\n");
        }

        String synthesisPrompt = """
                You are the orchestrator. Parallel specialist agents produced these results
                for the task below. Synthesise them into one coherent, structured response.
                Eliminate duplication, prioritise critical findings.

                ORIGINAL TASK:
                %s

                WORKER OUTPUTS:
                %s
                """.formatted(request.task(), merged);

        InferenceResponse resp = sdk.createCompletion(InferenceRequest.builder()
                .model(model)
                .message(Message.system(promptBuilder.buildMinimalPrompt()))
                .messages(List.of(Message.user(synthesisPrompt)))
                .temperature(0.5)
                .maxTokens(config.maxTokens())
                .streaming(false)
                .build());

        return resp.getContent() != null ? resp.getContent() : "";
    }

    private String resolveModel(AgentRequest request) {
        return (request.model() != null && !request.model().isBlank())
                ? request.model() : config.defaultModel();
    }

    private record SubTask(String name, String task) {}

    private record WorkerResult(String name, String output, boolean success,
                                 List<ToolResult> toolResults, Duration elapsed) {}
}

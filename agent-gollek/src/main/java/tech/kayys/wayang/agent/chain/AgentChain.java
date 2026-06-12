package tech.kayys.gamelan.agent.chain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.session.ConversationSession;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * AgentChain — composable, typed agent pipelines with branching and parallel execution.
 *
 * <h2>What is a chain</h2>
 * A chain is a sequence of composable steps where each step:
 * <ul>
 *   <li>Receives a typed input (plain text, structured data, or prior step's output)</li>
 *   <li>Produces a typed output consumed by the next step</li>
 *   <li>Can branch (multiple steps run in parallel, results merged)</li>
 *   <li>Can short-circuit (skip remaining steps based on a condition)</li>
 * </ul>
 *
 * <h2>Step types</h2>
 * <pre>
 * LLM_CALL   — calls the LLM with a templated prompt; output = LLM response
 * TRANSFORM  — applies a pure Java function to transform the output
 * FILTER     — evaluates a predicate; stops chain if false
 * BRANCH     — runs multiple sub-chains in parallel; collects all outputs
 * RETRY      — wraps a step with retry logic
 * CACHE      — caches step output keyed by input hash
 * VALIDATE   — asserts a condition about the output; fails chain if false
 * </pre>
 *
 * <h2>Example — Summarize → Classify → Route</h2>
 * <pre>
 * AgentChain.builder("triage")
 *     .llm("summarize", "Summarize in one sentence: {{input}}")
 *     .llm("classify", "Classify as BUG|FEATURE|QUESTION: {{input}}")
 *     .transform("route", s -> routingMap.get(s.trim()))
 *     .filter("valid", s -> s != null)
 *     .build()
 *     .run("My application throws NPE on startup after upgrade");
 * </pre>
 *
 * <h2>Parallel branch example</h2>
 * <pre>
 * AgentChain.builder("multi-review")
 *     .branch("parallel-review",
 *         AgentChain.step("bugs",     "Find bugs in: {{input}}"),
 *         AgentChain.step("security", "Find security issues in: {{input}}"),
 *         AgentChain.step("perf",     "Find performance issues in: {{input}}"))
 *     .llm("synthesize", "Synthesize these reviews: {{input}}")
 *     .build()
 *     .run(codeToReview);
 * </pre>
 */
@ApplicationScoped
public class AgentChain {

    private static final Logger log = LoggerFactory.getLogger(AgentChain.class);

    @Inject SingleAgentOrchestrator orchestrator;
    @Inject GamelanConfig            config;
    @Inject AgentTelemetry           telemetry;

    // ── Builder entry point ────────────────────────────────────────────────

    public Builder builder(String chainName) {
        return new Builder(chainName, this);
    }

    /** Creates a single LLM step for use in branch() */
    public static ChainStep step(String name, String promptTemplate) {
        return new ChainStep(name, StepType.LLM_CALL, promptTemplate, null, null, null, 1, false);
    }

    // ── Execution ─────────────────────────────────────────────────────────

    ChainResult execute(ChainDefinition def, String input) {
        Instant start = Instant.now();
        log.info("[chain] '{}' starting: {} steps, input='{}'",
                def.name(), def.steps().size(), truncate(input, 60));
        telemetry.count("chain.execute.total");

        String current = input;
        List<StepRecord> records = new ArrayList<>();
        boolean shortCircuited = false;

        for (ChainStep step : def.steps()) {
            Instant stepStart = Instant.now();
            try {
                String output = executeStep(step, current, def);

                Duration stepElapsed = Duration.between(stepStart, Instant.now());
                records.add(new StepRecord(step.name(), step.type(), current,
                        output, true, null, stepElapsed));

                if (step.type() == StepType.FILTER && (output == null || "false".equals(output))) {
                    log.info("[chain] '{}' short-circuited at filter step '{}'", def.name(), step.name());
                    shortCircuited = true;
                    break;
                }

                current = output;
                telemetry.count("chain.step." + step.type().name().toLowerCase());

            } catch (Exception e) {
                Duration stepElapsed = Duration.between(stepStart, Instant.now());
                records.add(new StepRecord(step.name(), step.type(), current,
                        null, false, e.getMessage(), stepElapsed));
                log.error("[chain] step '{}' failed in chain '{}': {}", step.name(), def.name(), e.getMessage());

                Duration elapsed = Duration.between(start, Instant.now());
                return new ChainResult(def.name(), false, null, e.getMessage(),
                        records, shortCircuited, elapsed);
            }
        }

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("[chain] '{}' completed in {}ms: {} steps, short-circuited={}",
                def.name(), elapsed.toMillis(), records.size(), shortCircuited);
        telemetry.count("chain.execute.success");

        return new ChainResult(def.name(), true, current, null,
                records, shortCircuited, elapsed);
    }

    private String executeStep(ChainStep step, String input, ChainDefinition def) {
        // Template substitution: replace {{input}} with current input
        String rendered = step.promptTemplate() != null
                ? step.promptTemplate().replace("{{input}}", input) : input;

        return switch (step.type()) {
            case LLM_CALL -> callLlm(rendered, step.name());

            case TRANSFORM -> {
                if (step.transformFn() == null) yield input;
                String result = step.transformFn().apply(input);
                yield result != null ? result : "";
            }

            case FILTER -> {
                if (step.filterPredicate() == null) yield "true";
                yield step.filterPredicate().test(input) ? "true" : "false";
            }

            case VALIDATE -> {
                if (step.filterPredicate() == null) yield input;
                if (!step.filterPredicate().test(input)) {
                    throw new IllegalStateException("Validation failed in step '" + step.name() +
                            "': condition not met for output: " + truncate(input, 100));
                }
                yield input;
            }

            case BRANCH -> {
                if (step.subSteps() == null || step.subSteps().isEmpty()) yield input;
                yield executeBranch(step.subSteps(), input, def.name());
            }

            case CACHE -> {
                String cacheKey = step.name() + ":" + input.hashCode();
                if (def.cache().containsKey(cacheKey)) {
                    log.debug("[chain] cache hit for step '{}'", step.name());
                    telemetry.count("chain.cache.hit");
                    yield def.cache().get(cacheKey);
                }
                String result = callLlm(rendered, step.name());
                def.cache().put(cacheKey, result);
                yield result;
            }

            case RETRY -> {
                Exception last = null;
                for (int attempt = 1; attempt <= step.maxRetries(); attempt++) {
                    try {
                        yield callLlm(rendered, step.name());
                    } catch (Exception e) {
                        last = e;
                        log.warn("[chain] step '{}' attempt {}/{} failed", step.name(), attempt, step.maxRetries());
                        if (attempt < step.maxRetries()) {
                            try { Thread.sleep(200L * attempt); } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
                throw new RuntimeException("Step '" + step.name() + "' failed after " + step.maxRetries() + " attempts", last);
            }
        };
    }

    private String executeBranch(List<ChainStep> subSteps, String input, String parentName) {
        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<String>> futures = subSteps.stream()
                .map(s -> exec.submit(() -> executeStep(s, input, new ChainDefinition(parentName, subSteps, Map.of()))))
                .toList();
        exec.shutdown();

        List<String> outputs = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                outputs.add(futures.get(i).get(60, TimeUnit.SECONDS));
            } catch (Exception e) {
                outputs.add("[Branch step " + subSteps.get(i).name() + " failed: " + e.getMessage() + "]");
            }
        }

        // Merge outputs with labeled headers
        StringBuilder merged = new StringBuilder();
        for (int i = 0; i < outputs.size(); i++) {
            merged.append("### ").append(subSteps.get(i).name().toUpperCase()).append("\n");
            merged.append(outputs.get(i)).append("\n\n");
        }
        return merged.toString().strip();
    }

    private String callLlm(String prompt, String stepName) {
        try {
            OrchestratorResult result = orchestrator.execute(
                    AgentRequest.builder(prompt)
                            .model(config.defaultModel())
                            .session(new ConversationSession(null, false, config.tokenBudget()))
                            .stream(false).maxSteps(1).build());
            if (!result.success()) throw new RuntimeException(result.error());
            return result.answer();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("LLM call failed in step '" + stepName + "': " + e.getMessage(), e);
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum StepType { LLM_CALL, TRANSFORM, FILTER, VALIDATE, BRANCH, CACHE, RETRY }

    public record ChainStep(
            String              name,
            StepType            type,
            String              promptTemplate,
            Function<String, String>  transformFn,
            Predicate<String>   filterPredicate,
            List<ChainStep>     subSteps,
            int                 maxRetries,
            boolean             optional
    ) {}

    public record ChainDefinition(
            String              name,
            List<ChainStep>     steps,
            Map<String, String> cache
    ) {}

    public record StepRecord(
            String   stepName,
            StepType type,
            String   input,
            String   output,
            boolean  success,
            String   error,
            Duration elapsed
    ) {}

    public record ChainResult(
            String           chainName,
            boolean          success,
            String           output,
            String           error,
            List<StepRecord> steps,
            boolean          shortCircuited,
            Duration         elapsed
    ) {
        public String summary() {
            return String.format("Chain[%s]: %s | %d steps | %dms%s",
                    chainName, success ? "SUCCESS" : "FAILED",
                    steps.size(), elapsed.toMillis(),
                    shortCircuited ? " | short-circuited" : "");
        }
    }

    // ── Executable chain ──────────────────────────────────────────────────

    /** A fully built, executable chain. */
    public record ExecutableChain(ChainDefinition definition, AgentChain engine) {
        public ChainResult run(String input) {
            return engine.execute(definition, input);
        }

        /** Runs the chain asynchronously. */
        public CompletableFuture<ChainResult> runAsync(String input) {
            return CompletableFuture.supplyAsync(
                    () -> engine.execute(definition, input),
                    Executors.newVirtualThreadPerTaskExecutor());
        }

        /** Runs the chain on multiple inputs in parallel and collects results. */
        public List<ChainResult> runBatch(List<String> inputs) {
            ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
            List<Future<ChainResult>> futures = inputs.stream()
                    .map(i -> exec.submit(() -> engine.execute(definition, i)))
                    .toList();
            exec.shutdown();
            return futures.stream().map(f -> {
                try { return f.get(120, TimeUnit.SECONDS); }
                catch (Exception e) { return new ChainResult(definition.name(), false,
                        null, e.getMessage(), List.of(), false, Duration.ZERO); }
            }).toList();
        }
    }

    // ── Fluent Builder ─────────────────────────────────────────────────────

    public static final class Builder {
        private final String         name;
        private final AgentChain     engine;
        private final List<ChainStep> steps = new ArrayList<>();
        private final Map<String, String> cache = new ConcurrentHashMap<>();

        Builder(String name, AgentChain engine) { this.name = name; this.engine = engine; }

        /** Adds an LLM call step. Use {{input}} in the prompt for substitution. */
        public Builder llm(String stepName, String promptTemplate) {
            steps.add(new ChainStep(stepName, StepType.LLM_CALL, promptTemplate,
                    null, null, null, 1, false));
            return this;
        }

        /** Adds a Java transform step (no LLM). */
        public Builder transform(String stepName, Function<String, String> fn) {
            steps.add(new ChainStep(stepName, StepType.TRANSFORM, null,
                    fn, null, null, 1, false));
            return this;
        }

        /** Adds a filter step. If predicate returns false, chain stops. */
        public Builder filter(String stepName, Predicate<String> predicate) {
            steps.add(new ChainStep(stepName, StepType.FILTER, null,
                    null, predicate, null, 1, false));
            return this;
        }

        /** Adds a validation step. Throws if predicate is false. */
        public Builder validate(String stepName, Predicate<String> predicate) {
            steps.add(new ChainStep(stepName, StepType.VALIDATE, null,
                    null, predicate, null, 1, false));
            return this;
        }

        /** Adds a parallel branch: all sub-steps run concurrently, outputs merged. */
        @SafeVarargs
        public final Builder branch(String stepName, ChainStep... subSteps) {
            steps.add(new ChainStep(stepName, StepType.BRANCH, null,
                    null, null, List.of(subSteps), 1, false));
            return this;
        }

        /** Adds a cached LLM call. Same input → same output (no new LLM call). */
        public Builder cached(String stepName, String promptTemplate) {
            steps.add(new ChainStep(stepName, StepType.CACHE, promptTemplate,
                    null, null, null, 1, false));
            return this;
        }

        /** Adds an LLM call with automatic retry on failure. */
        public Builder withRetry(String stepName, String promptTemplate, int maxRetries) {
            steps.add(new ChainStep(stepName, StepType.RETRY, promptTemplate,
                    null, null, null, maxRetries, false));
            return this;
        }

        public ExecutableChain build() {
            return new ExecutableChain(
                    new ChainDefinition(name, List.copyOf(steps), cache), engine);
        }
    }
}

package tech.kayys.gamelan.workflow.reactive;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.resilience.circuit.AgentResilienceKit;
import tech.kayys.gamelan.session.ConversationSession;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * ReactiveWorkflowEngine — event-driven, non-blocking agent workflow execution.
 *
 * <h2>Beyond sequential and parallel</h2>
 * The {@link tech.kayys.gamelan.execution.dag.DagExecutionEngine} handles static
 * dependency graphs. The ReactiveWorkflowEngine handles dynamic, event-driven flows:
 * <ul>
 *   <li>A step can emit events that trigger other steps</li>
 *   <li>Steps can wait for external events (file changes, CI results, user input)</li>
 *   <li>Long-running workflows that pause, resume, and compensate</li>
 *   <li>Fan-out patterns where one event triggers many parallel handlers</li>
 * </ul>
 *
 * <h2>Saga Pattern</h2>
 * For workflows that span multiple systems (git commit, CI run, PR creation),
 * the Saga pattern ensures consistency through compensating transactions:
 * <pre>
 * Saga("deploy-hotfix"):
 *   Step 1: write-fix       → compensate: revert-fix
 *   Step 2: commit-code     → compensate: git-revert
 *   Step 3: push-branch     → compensate: delete-branch
 *   Step 4: create-pr       → compensate: close-pr
 *
 * If step 4 fails → run compensations for steps 3, 2, 1 in reverse order.
 * </pre>
 *
 * <h2>Event bus</h2>
 * Steps communicate through a typed event bus. Events carry a payload (string)
 * and a type tag. Subscribers filter by event type and are invoked on virtual threads.
 *
 * <h2>Reactive primitives</h2>
 * <ul>
 *   <li>{@code emit(type, payload)} — publish an event</li>
 *   <li>{@code on(type, handler)} — subscribe to events</li>
 *   <li>{@code await(type, timeout)} — block until event arrives</li>
 *   <li>{@code saga(steps)} — execute with automatic compensation on failure</li>
 * </ul>
 */
@ApplicationScoped
public class ReactiveWorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(ReactiveWorkflowEngine.class);

    @Inject SingleAgentOrchestrator orchestrator;
    @Inject GamelanConfig            config;
    @Inject AgentTelemetry           telemetry;
    @Inject AgentResilienceKit        resilience;

    // Event bus: event type → list of subscriber queues
    private final Map<String, List<BlockingQueue<WorkflowEvent>>> subscribers = new ConcurrentHashMap<>();
    // Running workflows
    private final Map<String, WorkflowExecution>                  executions  = new ConcurrentHashMap<>();

    // ── Event bus ──────────────────────────────────────────────────────────

    /**
     * Emits an event to all subscribers of that type.
     */
    public void emit(String workflowId, String eventType, String payload) {
        WorkflowEvent event = new WorkflowEvent(workflowId, eventType, payload, Instant.now());
        String key = workflowId + ":" + eventType;
        List<BlockingQueue<WorkflowEvent>> subs = subscribers.getOrDefault(key, List.of());
        subs.forEach(q -> q.offer(event));

        log.debug("[reactive] emitted event {}:{} payload='{}'",
                workflowId, eventType, truncate(payload, 50));
        telemetry.count("workflow.event.emitted");
    }

    /**
     * Subscribes to events of a given type within a workflow.
     * Returns an AutoCloseable subscription.
     */
    public EventSubscription on(String workflowId, String eventType,
                                 Consumer<WorkflowEvent> handler) {
        String key = workflowId + ":" + eventType;
        BlockingQueue<WorkflowEvent> queue = new LinkedBlockingQueue<>(1000);
        subscribers.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(queue);

        // Process events on a virtual thread
        AtomicBoolean active = new AtomicBoolean(true);
        Thread.ofVirtual().start(() -> {
            while (active.get()) {
                try {
                    WorkflowEvent event = queue.poll(1, TimeUnit.SECONDS);
                    if (event != null) handler.accept(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        return new EventSubscription(key, queue, active, subscribers);
    }

    /**
     * Blocks until an event of the given type arrives or timeout elapses.
     */
    public Optional<WorkflowEvent> await(String workflowId, String eventType,
                                           Duration timeout) {
        String key = workflowId + ":" + eventType;
        BlockingQueue<WorkflowEvent> queue = new LinkedBlockingQueue<>(10);
        subscribers.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(queue);
        try {
            WorkflowEvent event = queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return Optional.ofNullable(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            List<BlockingQueue<WorkflowEvent>> subs = subscribers.get(key);
            if (subs != null) subs.remove(queue);
        }
    }

    // ── Saga execution ─────────────────────────────────────────────────────

    /**
     * Executes a saga — a sequence of steps with compensating transactions.
     * If any step fails, all completed steps' compensations are executed in reverse.
     *
     * @param sagaId    unique identifier for this saga
     * @param steps     ordered list of saga steps
     * @return the saga execution result
     */
    public SagaResult executeSaga(String sagaId, List<SagaStep> steps) {
        log.info("[saga] starting '{}': {} steps", sagaId, steps.size());
        Instant start = Instant.now();

        List<CompletedStep> completed = new ArrayList<>();
        String lastOutput = "";

        for (SagaStep step : steps) {
            log.info("[saga] executing step '{}' in saga '{}'", step.name(), sagaId);
            telemetry.count("saga.step.started");

            try {
                String output = executeStep(step, lastOutput, sagaId);
                completed.add(new CompletedStep(step, output));
                lastOutput = output;
                emit(sagaId, "step.completed", step.name() + ":" + truncate(output, 100));
                log.info("[saga] step '{}' completed", step.name());
                telemetry.count("saga.step.completed");

            } catch (Exception e) {
                log.error("[saga] step '{}' failed: {}", step.name(), e.getMessage());
                telemetry.count("saga.step.failed");

                // Execute compensations in reverse
                SagaCompensationResult comp = compensate(sagaId, completed);
                Duration elapsed = Duration.between(start, Instant.now());
                return SagaResult.failed(sagaId, step.name(), e.getMessage(), comp, completed, elapsed);
            }
        }

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("[saga] '{}' completed successfully in {}ms", sagaId, elapsed.toMillis());
        return SagaResult.success(sagaId, completed, lastOutput, elapsed);
    }

    // ── Reactive workflow ──────────────────────────────────────────────────

    /**
     * Runs a reactive workflow defined as a graph of event-triggered steps.
     * Steps are triggered by events and can emit events to trigger other steps.
     *
     * @param workflowId   unique workflow identifier
     * @param definition   the workflow definition
     * @param initialEvent the event that starts the workflow
     * @return a future that completes when the workflow finishes
     */
    public CompletableFuture<WorkflowResult> runReactive(
            String workflowId, ReactiveWorkflowDefinition definition,
            WorkflowEvent initialEvent) {

        WorkflowExecution exec = new WorkflowExecution(workflowId, definition);
        executions.put(workflowId, exec);

        CompletableFuture<WorkflowResult> future = new CompletableFuture<>();

        Thread.ofVirtual().start(() -> {
            try {
                // Set up subscriptions for all step triggers
                List<EventSubscription> subs = new ArrayList<>();
                for (ReactiveStep step : definition.steps()) {
                    EventSubscription sub = on(workflowId, step.triggerEvent(), event -> {
                        if (!exec.isStepCompleted(step.name()) && step.condition().test(event)) {
                            executeReactiveStep(workflowId, step, event, exec);
                        }
                    });
                    subs.add(sub);
                }

                // Emit initial event to start the workflow
                emit(workflowId, initialEvent.eventType(), initialEvent.payload());

                // Wait for terminal event or timeout
                Optional<WorkflowEvent> terminal = await(workflowId, "workflow.done",
                        definition.timeout());

                // Clean up subscriptions
                subs.forEach(EventSubscription::close);

                boolean success = terminal.isPresent();
                future.complete(new WorkflowResult(workflowId, success,
                        exec.completedSteps(), exec.stepOutputs(),
                        Duration.between(exec.startedAt(), Instant.now())));

            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                executions.remove(workflowId);
            }
        });

        return future;
    }

    /**
     * Returns currently running workflows.
     */
    public List<WorkflowStatus> runningWorkflows() {
        return executions.values().stream()
                .map(e -> new WorkflowStatus(e.workflowId(),
                        e.completedSteps().size(), e.definition().steps().size(),
                        Duration.between(e.startedAt(), Instant.now())))
                .toList();
    }

    // ── Private ────────────────────────────────────────────────────────────

    private String executeStep(SagaStep step, String priorOutput, String sagaId) {
        String task = step.task().replace("{{prior}}", priorOutput);
        AgentRequest req = AgentRequest.builder(task)
                .model(config.defaultModel())
                .session(new ConversationSession(null, false, config.tokenBudget()))
                .stream(false).maxSteps(step.maxIterations()).build();

        OrchestratorResult result = resilience.withRetry(
                "saga." + sagaId + "." + step.name(),
                step.maxRetries(), 500,
                e -> !e.getMessage().contains("cancelled"),
                () -> orchestrator.execute(req));

        if (!result.success()) throw new RuntimeException(result.error());
        return result.answer();
    }

    private SagaCompensationResult compensate(String sagaId, List<CompletedStep> completed) {
        log.warn("[saga] compensating {} steps for saga '{}'", completed.size(), sagaId);
        List<String> compensated = new ArrayList<>();
        List<String> failedComps = new ArrayList<>();

        for (int i = completed.size() - 1; i >= 0; i--) {
            CompletedStep cs = completed.get(i);
            if (cs.step().compensation() == null) continue;

            try {
                String compTask = cs.step().compensation()
                        .replace("{{original_output}}", cs.output());
                AgentRequest req = AgentRequest.builder(compTask)
                        .model(config.defaultModel())
                        .session(new ConversationSession(null, false, config.tokenBudget()))
                        .stream(false).maxSteps(5).build();
                orchestrator.execute(req);
                compensated.add(cs.step().name());
                emit(sagaId, "step.compensated", cs.step().name());
            } catch (Exception e) {
                failedComps.add(cs.step().name() + ":" + e.getMessage());
                log.error("[saga] compensation for '{}' failed: {}", cs.step().name(), e.getMessage());
            }
        }
        return new SagaCompensationResult(compensated, failedComps);
    }

    private void executeReactiveStep(String workflowId, ReactiveStep step,
                                      WorkflowEvent trigger, WorkflowExecution exec) {
        log.debug("[reactive] triggering step '{}' from event '{}'",
                step.name(), trigger.eventType());
        telemetry.count("workflow.reactive.step");

        try {
            String task = step.task().replace("{{event}}", trigger.payload());
            AgentRequest req = AgentRequest.builder(task)
                    .model(config.defaultModel())
                    .session(new ConversationSession(null, false, config.tokenBudget()))
                    .stream(false).maxSteps(10).build();

            OrchestratorResult result = orchestrator.execute(req);
            exec.markCompleted(step.name(), result.answer());

            // Emit the step's output event
            emit(workflowId, step.outputEvent(), result.answer());

            // Check if this was the terminal step
            if (step.terminal()) {
                emit(workflowId, "workflow.done", result.answer());
            }
        } catch (Exception e) {
            log.error("[reactive] step '{}' failed: {}", step.name(), e.getMessage());
            emit(workflowId, "step.failed", step.name() + ":" + e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record WorkflowEvent(
            String  workflowId,
            String  eventType,
            String  payload,
            Instant emittedAt
    ) {}

    /** A step in a saga with optional compensation. */
    public record SagaStep(
            String name,
            String task,           // may contain {{prior}} placeholder
            String compensation,   // null = not compensatable; may contain {{original_output}}
            int    maxRetries,
            int    maxIterations
    ) {
        public static SagaStep of(String name, String task) {
            return new SagaStep(name, task, null, 1, 5);
        }
        public static SagaStep withCompensation(String name, String task, String comp) {
            return new SagaStep(name, task, comp, 2, 5);
        }
    }

    /** A step in a reactive workflow, triggered by an event. */
    public record ReactiveStep(
            String                       name,
            String                       triggerEvent,
            String                       outputEvent,
            String                       task,
            Predicate<WorkflowEvent>     condition,
            boolean                      terminal
    ) {
        public static ReactiveStep of(String name, String trigger, String output, String task) {
            return new ReactiveStep(name, trigger, output, task, e -> true, false);
        }
        public static ReactiveStep terminal(String name, String trigger, String task) {
            return new ReactiveStep(name, trigger, "workflow.done", task, e -> true, true);
        }
    }

    public record ReactiveWorkflowDefinition(
            String             name,
            List<ReactiveStep> steps,
            Duration           timeout
    ) {}

    public record CompletedStep(SagaStep step, String output) {}

    public record SagaCompensationResult(
            List<String> compensated,
            List<String> failedCompensations
    ) {}

    public record SagaResult(
            String                  sagaId,
            boolean                 success,
            String                  failedStep,
            String                  failureReason,
            SagaCompensationResult  compensation,
            List<CompletedStep>     completedSteps,
            String                  finalOutput,
            Duration                elapsed
    ) {
        static SagaResult success(String id, List<CompletedStep> steps,
                                   String output, Duration d) {
            return new SagaResult(id, true, null, null, null, steps, output, d);
        }
        static SagaResult failed(String id, String failedStep, String reason,
                                  SagaCompensationResult comp, List<CompletedStep> steps, Duration d) {
            return new SagaResult(id, false, failedStep, reason, comp, steps, "", d);
        }
        public String summary() {
            return success
                    ? String.format("Saga[%s]: SUCCESS %d steps in %dms",
                            sagaId, completedSteps.size(), elapsed.toMillis())
                    : String.format("Saga[%s]: FAILED at '%s' — %s | compensated=%d",
                            sagaId, failedStep, failureReason,
                            compensation != null ? compensation.compensated().size() : 0);
        }
    }

    public record WorkflowResult(
            String              workflowId,
            boolean             success,
            List<String>        completedSteps,
            Map<String, String> stepOutputs,
            Duration            elapsed
    ) {}

    public record WorkflowStatus(
            String   workflowId,
            int      completedSteps,
            int      totalSteps,
            Duration elapsed
    ) {}

    /** Internal tracking of a running reactive workflow. */
    private static final class WorkflowExecution {
        private final String                       workflowId;
        private final ReactiveWorkflowDefinition   definition;
        private final Instant                      startedAt    = Instant.now();
        private final Set<String>                  completed    = ConcurrentHashMap.newKeySet();
        private final Map<String, String>          outputs      = new ConcurrentHashMap<>();

        WorkflowExecution(String id, ReactiveWorkflowDefinition def) {
            this.workflowId = id; this.definition = def;
        }

        void markCompleted(String step, String output) {
            completed.add(step); outputs.put(step, output);
        }
        boolean isStepCompleted(String step) { return completed.contains(step); }

        String workflowId()             { return workflowId; }
        ReactiveWorkflowDefinition definition() { return definition; }
        Instant startedAt()             { return startedAt; }
        List<String> completedSteps()   { return List.copyOf(completed); }
        Map<String, String> stepOutputs() { return Collections.unmodifiableMap(outputs); }
    }

    /** Handle for managing an event subscription. */
    public static final class EventSubscription implements AutoCloseable {
        private final String                                  key;
        private final BlockingQueue<WorkflowEvent>            queue;
        private final AtomicBoolean                           active;
        private final Map<String, List<BlockingQueue<WorkflowEvent>>> registry;

        EventSubscription(String key, BlockingQueue<WorkflowEvent> queue,
                          AtomicBoolean active,
                          Map<String, List<BlockingQueue<WorkflowEvent>>> registry) {
            this.key = key; this.queue = queue;
            this.active = active; this.registry = registry;
        }

        @Override
        public void close() {
            active.set(false);
            List<BlockingQueue<WorkflowEvent>> subs = registry.get(key);
            if (subs != null) subs.remove(queue);
        }
    }
}

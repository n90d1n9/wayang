package tech.kayys.wayang.agent.spi;


import java.time.Instant;
import java.util.*;

/**
 * Immutable snapshot of the agent's state at a given reasoning step.
 */
public final class AgentState {

    private final String runId;
    private final AgentRequest request;
    private final int step;
    private final Phase phase;
    private final List<ReasoningStep> history;
    private final Map<String, Object> workingMemory;
    private final String lastThought;
    private final AgentAction pendingAction;
    private final String lastObservation;
    private final String finalAnswer;
    private final String errorMessage;
    private final Instant startedAt;
    private final Instant updatedAt;

    private AgentState(Builder b) {
        this.runId = b.runId;
        this.request = b.request;
        this.step = b.step;
        this.phase = b.phase;
        this.history = Collections.unmodifiableList(new ArrayList<>(b.history));
        this.workingMemory = Collections.unmodifiableMap(new HashMap<>(b.workingMemory));
        this.lastThought = b.lastThought;
        this.pendingAction = b.pendingAction;
        this.lastObservation = b.lastObservation;
        this.finalAnswer = b.finalAnswer;
        this.errorMessage = b.errorMessage;
        this.startedAt = b.startedAt != null ? b.startedAt : Instant.now();
        this.updatedAt = Instant.now();
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String getRunId() {
        return runId;
    }

    public AgentRequest getRequest() {
        return request;
    }

    public int getStep() {
        return step;
    }

    public Phase getPhase() {
        return phase;
    }

    public List<ReasoningStep> getHistory() {
        return history;
    }

    public Map<String, Object> getWorkingMemory() {
        return workingMemory;
    }

    public Optional<String> getLastThought() {
        return Optional.ofNullable(lastThought);
    }

    public Optional<AgentAction> getPendingAction() {
        return Optional.ofNullable(pendingAction);
    }

    public Optional<String> getLastObservation() {
        return Optional.ofNullable(lastObservation);
    }

    public Optional<String> getFinalAnswer() {
        return Optional.ofNullable(finalAnswer);
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isComplete() {
        return phase == Phase.COMPLETE;
    }

    public boolean isFailed() {
        return phase == Phase.FAILED;
    }

    public boolean isTerminal() {
        return isComplete() || isFailed();
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public boolean atMaxSteps() {
        return step >= request.getMaxSteps();
    }

    // ── State Transitions ─────────────────────────────────────────────────────

    public AgentState withThought(String thought) {
        return toBuilder().step(step + 1).phase(Phase.THINKING).lastThought(thought).build();
    }

    public AgentState withAction(AgentAction action) {
        return toBuilder().phase(Phase.ACTING).pendingAction(action).build();
    }

    public AgentState withObservation(String observation, ReasoningStep completedStep) {
        return toBuilder()
                .phase(Phase.OBSERVING)
                .pendingAction(null)
                .lastObservation(observation)
                .addHistoryStep(completedStep)
                .build();
    }

    public AgentState withFinalAnswer(String answer) {
        return toBuilder().phase(Phase.COMPLETE).finalAnswer(answer).build();
    }

    public AgentState withError(String error) {
        return toBuilder().phase(Phase.FAILED).errorMessage(error).build();
    }

    public AgentState withMemory(String key, Object value) {
        Map<String, Object> newMem = new HashMap<>(workingMemory);
        newMem.put(key, value);
        return toBuilder().workingMemory(newMem).build();
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static AgentState initial(AgentRequest request) {
        return new Builder()
                .runId(UUID.randomUUID().toString())
                .request(request)
                .step(0)
                .phase(Phase.THINKING)
                .startedAt(Instant.now())
                .build();
    }

    public Builder toBuilder() {
        return new Builder()
                .runId(runId).request(request).step(step).phase(phase)
                .history(history).workingMemory(workingMemory).lastThought(lastThought)
                .pendingAction(pendingAction).lastObservation(lastObservation)
                .finalAnswer(finalAnswer).errorMessage(errorMessage).startedAt(startedAt);
    }

    public static final class Builder {
        private String runId;
        private AgentRequest request;
        private int step;
        private Phase phase = Phase.THINKING;
        private final List<ReasoningStep> history = new ArrayList<>();
        private Map<String, Object> workingMemory = new HashMap<>();
        private String lastThought;
        private AgentAction pendingAction;
        private String lastObservation;
        private String finalAnswer;
        private String errorMessage;
        private Instant startedAt;

        public Builder runId(String v) {
            runId = v;
            return this;
        }

        public Builder request(AgentRequest v) {
            request = v;
            return this;
        }

        public Builder step(int v) {
            step = v;
            return this;
        }

        public Builder phase(Phase v) {
            phase = v;
            return this;
        }

        public Builder history(List<ReasoningStep> v) {
            history.addAll(v);
            return this;
        }

        public Builder addHistoryStep(ReasoningStep v) {
            history.add(v);
            return this;
        }

        public Builder workingMemory(Map<String, Object> v) {
            workingMemory = v;
            return this;
        }

        public Builder lastThought(String v) {
            lastThought = v;
            return this;
        }

        public Builder pendingAction(AgentAction v) {
            pendingAction = v;
            return this;
        }

        public Builder lastObservation(String v) {
            lastObservation = v;
            return this;
        }

        public Builder finalAnswer(String v) {
            finalAnswer = v;
            return this;
        }

        public Builder errorMessage(String v) {
            errorMessage = v;
            return this;
        }

        public Builder startedAt(Instant v) {
            startedAt = v;
            return this;
        }

        public AgentState build() {
            return new AgentState(this);
        }
    }

    // ── Nested Types ──────────────────────────────────────────────────────────

    public enum Phase {
        PLANNING, THINKING, ACTING, OBSERVING, REFLECTING, COMPLETE, FAILED
    }

    public record AgentAction(
            String skillId,
            String rationale,
            Map<String, Object> inputs,
            Instant requestedAt) {
        public static AgentAction of(String skillId, Map<String, Object> inputs) {
            return new AgentAction(skillId, null, Map.copyOf(inputs), Instant.now());
        }
    }

    public record ReasoningStep(
            int stepNumber,
            String thought,
            AgentAction action,
            String observation,
            long durationMs,
            boolean successful) {
    }
}

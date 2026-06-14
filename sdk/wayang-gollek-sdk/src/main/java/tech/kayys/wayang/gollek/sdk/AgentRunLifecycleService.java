package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class AgentRunLifecycleService {

    private final AgentRunStore store;

    public AgentRunLifecycleService(AgentRunStore store) {
        this.store = store == null ? AgentRunStore.memory() : store;
    }

    public static AgentRunLifecycleService create(AgentRunStore store) {
        return new AgentRunLifecycleService(store);
    }

    public AgentRunStatus record(AgentRunResult result) {
        if (result == null) {
            return store.save(AgentRunStatus.unknown("", "Cannot record a null run result."));
        }
        Map<String, Object> metadata = new LinkedHashMap<>(result.metadata());
        metadata.put("successful", result.successful());
        metadata.put("stepCount", result.steps().size());
        return store.save(new AgentRunStatus(
                result.handle(),
                true,
                messageFor(result.handle()),
                metadata));
    }

    public AgentRunStatus status(String runId) {
        String normalizedRunId = SdkText.trimToEmpty(runId);
        return store.find(normalizedRunId)
                .orElseGet(() -> AgentRunStatus.unknown(
                        normalizedRunId,
                        "No run status is recorded for this run id."));
    }

    public AgentRunHistory history() {
        return history(AgentRunHistoryQuery.all());
    }

    public AgentRunHistory history(AgentRunHistoryQuery query) {
        AgentRunHistoryQuery normalized = query == null ? AgentRunHistoryQuery.all() : query;
        List<AgentRunStatus> matches = store.findAll().stream()
                .filter(normalized::matches)
                .toList();
        List<AgentRunStatus> runs = matches.stream()
                .skip(normalized.offset())
                .limit(normalized.limit())
                .toList();
        return new AgentRunHistory(
                normalized,
                runs,
                matches.size(),
                historyMessage(normalized, matches, runs));
    }

    public AgentRunStoreDiagnostics diagnostics() {
        return store.diagnostics();
    }

    public AgentRunStoreVerification verification() {
        return store.verification();
    }

    public AgentRunStoreCompactionPreview compactionPreview() {
        return store.compactionPreview();
    }

    public AgentRunStoreCompactionResult compact() {
        return store.compact();
    }

    public AgentRunEvents events(String runId) {
        return events(runId, AgentRunEventsQuery.all());
    }

    public AgentRunEvents events(String runId, AgentRunEventsQuery query) {
        String normalizedRunId = SdkText.trimToEmpty(runId);
        AgentRunEventsQuery normalized = query == null ? AgentRunEventsQuery.all() : query;
        List<AgentRunEvent> matches = store.events(normalizedRunId).stream()
                .filter(normalized::matches)
                .toList();
        List<AgentRunEvent> runEvents = AgentRunEventTimelines.latest(matches, normalized.limit());
        return new AgentRunEvents(
                normalizedRunId,
                normalized,
                runEvents,
                matches.size(),
                matches.isEmpty()
                        ? normalized.filtered()
                                ? "No run events match the query."
                                : "No run events are recorded for this run id."
                        : "Recorded run events.");
    }

    public AgentRunEventsFollowResult followEvents(
            String runId,
            AgentRunEventsFollowOptions options,
            Consumer<AgentRunEvents> eventConsumer) {
        return followEvents(runId, options, this::events, eventConsumer);
    }

    public AgentRunInspection inspect(String runId) {
        return inspect(runId, AgentRunEventsQuery.all());
    }

    public AgentRunInspection inspect(String runId, AgentRunEventsQuery query) {
        return inspection(runId, status(runId), events(runId, query));
    }

    public AgentRunForgetResult forget(String runId) {
        String normalizedRunId = SdkText.trimToEmpty(runId);
        Optional<AgentRunStatus> status = store.find(normalizedRunId);
        if (status.isEmpty()) {
            return AgentRunForgetResult.notFound(normalizedRunId, "No run status is recorded for this run id.");
        }
        boolean removed = store.remove(normalizedRunId);
        return removed
                ? AgentRunForgetResult.forgotten(status.get())
                : AgentRunForgetResult.notFound(normalizedRunId, "Run status could not be forgotten.");
    }

    public AgentRunCancelResult cancel(String runId, String reason) {
        String normalizedRunId = SdkText.trimToEmpty(runId);
        Optional<AgentRunStatus> status = store.find(normalizedRunId);
        if (status.isEmpty()) {
            return AgentRunCancelResult.notFound(normalizedRunId, "No run status is recorded for this run id.");
        }
        AgentRunStatus current = status.get();
        if (current.handle().terminal()) {
            return AgentRunCancelResult.notCancellable(
                    current,
                    "Run state is " + current.handle().state().name().toLowerCase().replace('_', ' ')
                            + " and cannot be cancelled.");
        }
        Map<String, Object> metadata = new LinkedHashMap<>(current.metadata());
        metadata.put("previousState", current.handle().state().name());
        String normalizedReason = SdkText.trimToEmpty(reason);
        if (!normalizedReason.isEmpty()) {
            metadata.put("reason", normalizedReason);
        }
        AgentRunStatus cancelled = store.save(new AgentRunStatus(
                new AgentRunHandle(
                        current.handle().runId(),
                        AgentRunState.CANCELLED,
                        current.handle().strategy()),
                true,
                "Run was cancelled.",
                metadata));
        return AgentRunCancelResult.cancelled(cancelled);
    }

    public AgentRunWaitResult waitForRun(String runId, AgentRunWaitOptions options) {
        return waitForRun(runId, options, this::status);
    }

    public static AgentRunInspection inspection(String runId, AgentRunStatus status, AgentRunEvents events) {
        String normalizedRunId = SdkText.trimToEmpty(runId);
        AgentRunStatus normalizedStatus = status == null
                ? AgentRunStatus.unknown(normalizedRunId, "Run status is unknown.")
                : status;
        AgentRunEvents normalizedEvents = events == null
                ? new AgentRunEvents(
                        normalizedRunId,
                        AgentRunEventsQuery.all(),
                        List.of(),
                        0,
                        "Run event storage is not configured for this Wayang SDK.")
                : events;
        String inspectionRunId = !normalizedStatus.handle().runId().isBlank()
                ? normalizedStatus.handle().runId()
                : SdkText.trimToDefault(normalizedEvents.runId(), normalizedRunId);
        return new AgentRunInspection(
                inspectionRunId,
                normalizedStatus,
                normalizedEvents,
                normalizedStatus.known() || !normalizedEvents.empty()
                        ? "Inspected Wayang run lifecycle."
                        : "No run lifecycle data is recorded for this run id.");
    }

    public static AgentRunWaitResult waitForRun(
            String runId,
            AgentRunWaitOptions options,
            Function<String, AgentRunStatus> statusLookup) {
        AgentRunWaitOptions waitOptions = options == null ? AgentRunWaitOptions.defaults() : options;
        String normalizedRunId = SdkText.trimToEmpty(runId);
        Function<String, AgentRunStatus> lookup = statusLookup == null
                ? id -> AgentRunStatus.unknown(id, "Run status is unknown.")
                : statusLookup;
        long started = System.nanoTime();
        long deadline = started + waitOptions.timeoutMillis() * 1_000_000;
        int attempts = 0;
        while (true) {
            attempts++;
            AgentRunStatus status = normalizeStatus(normalizedRunId, lookup.apply(normalizedRunId));
            long elapsedMillis = elapsedMillis(started);
            if (status.handle().terminal()) {
                return waitResult(
                        normalizedRunId,
                        status,
                        false,
                        attempts,
                        elapsedMillis,
                        "Run reached terminal state: "
                                + status.handle().state().name().toLowerCase().replace('_', ' ') + ".",
                        waitOptions);
            }
            if (!status.known()) {
                return waitResult(
                        normalizedRunId,
                        status,
                        false,
                        attempts,
                        elapsedMillis,
                        status.message(),
                        waitOptions);
            }
            if (elapsedMillis >= waitOptions.timeoutMillis() || waitOptions.timeoutMillis() == 0) {
                return waitResult(
                        normalizedRunId,
                        status,
                        true,
                        attempts,
                        elapsedMillis,
                        "Run did not reach a terminal state before timeout.",
                        waitOptions);
            }
            sleepUntilNextPoll(waitOptions, deadline);
        }
    }

    public static AgentRunEventsFollowResult followEvents(
            String runId,
            AgentRunEventsFollowOptions options,
            BiFunction<String, AgentRunEventsQuery, AgentRunEvents> eventsLookup,
            Consumer<AgentRunEvents> eventConsumer) {
        AgentRunEventsFollowOptions followOptions = options == null
                ? AgentRunEventsFollowOptions.defaults()
                : options;
        String normalizedRunId = SdkText.trimToEmpty(runId);
        BiFunction<String, AgentRunEventsQuery, AgentRunEvents> lookup = eventsLookup == null
                ? (id, query) -> new AgentRunEvents(
                        id,
                        query,
                        List.of(),
                        0,
                        "Run event storage is not configured for this Wayang SDK.")
                : eventsLookup;
        Consumer<AgentRunEvents> consumer = eventConsumer == null ? events -> { } : eventConsumer;
        AgentRunEventsQuery initialQuery = followOptions.query();
        AgentRunEventsQuery currentQuery = initialQuery;
        AgentRunEvents lastEvents = new AgentRunEvents(normalizedRunId, currentQuery, List.of(), 0, "");
        long started = System.nanoTime();
        int polls = 0;
        while (polls < followOptions.maxPolls()) {
            polls++;
            lastEvents = normalizeEvents(normalizedRunId, currentQuery, lookup.apply(normalizedRunId, currentQuery));
            consumer.accept(lastEvents);
            AgentRunEventsQuery nextQuery = advanceQuery(currentQuery, lastEvents);
            Optional<AgentRunState> terminalState = terminalState(lastEvents);
            if (terminalState.isPresent()) {
                return followResult(
                        normalizedRunId,
                        initialQuery,
                        nextQuery,
                        lastEvents,
                        false,
                        polls,
                        elapsedMillis(started),
                        "Run events reached terminal state: "
                                + AgentRunStates.wireName(terminalState.get()).replace('_', ' ') + ".",
                        followOptions);
            }
            currentQuery = nextQuery;
            if (polls < followOptions.maxPolls()) {
                sleepUntilNextEventPoll(followOptions);
            }
        }
        return followResult(
                normalizedRunId,
                initialQuery,
                currentQuery,
                lastEvents,
                true,
                polls,
                elapsedMillis(started),
                "Run events did not reach a terminal state before max polls.",
                followOptions);
    }

    private static AgentRunStatus normalizeStatus(String runId, AgentRunStatus status) {
        return status == null ? AgentRunStatus.unknown(runId, "Run status is unknown.") : status;
    }

    private static AgentRunEvents normalizeEvents(
            String runId,
            AgentRunEventsQuery query,
            AgentRunEvents events) {
        return events == null
                ? new AgentRunEvents(
                        runId,
                        query,
                        List.of(),
                        0,
                        "Run event storage returned no event envelope.")
                : events;
    }

    private static AgentRunEventsQuery advanceQuery(AgentRunEventsQuery currentQuery, AgentRunEvents events) {
        return new AgentRunEventsQuery(
                currentQuery.state(),
                currentQuery.type(),
                Math.max(currentQuery.afterSequence(), events.nextAfterSequence()),
                currentQuery.limit());
    }

    private static Optional<AgentRunState> terminalState(AgentRunEvents events) {
        return events.events().stream()
                .map(AgentRunEvent::state)
                .filter(AgentRunState::terminal)
                .findFirst();
    }

    private static AgentRunWaitResult waitResult(
            String runId,
            AgentRunStatus status,
            boolean timedOut,
            int attempts,
            long elapsedMillis,
            String message,
            AgentRunWaitOptions options) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("timeoutMillis", options.timeoutMillis());
        metadata.put("pollMillis", options.pollMillis());
        return new AgentRunWaitResult(
                runId,
                status,
                status.handle().terminal(),
                timedOut,
                attempts,
                elapsedMillis,
                message,
                metadata);
    }

    private static AgentRunEventsFollowResult followResult(
            String runId,
            AgentRunEventsQuery initialQuery,
            AgentRunEventsQuery nextQuery,
            AgentRunEvents lastEvents,
            boolean maxPollsReached,
            int polls,
            long elapsedMillis,
            String message,
            AgentRunEventsFollowOptions options) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("maxPolls", options.maxPolls());
        metadata.put("pollMillis", options.pollMillis());
        return new AgentRunEventsFollowResult(
                runId,
                initialQuery,
                nextQuery,
                lastEvents,
                false,
                maxPollsReached,
                polls,
                elapsedMillis,
                message,
                metadata);
    }

    private static void sleepUntilNextPoll(AgentRunWaitOptions options, long deadline) {
        long remainingMillis = Math.max(0, (deadline - System.nanoTime()) / 1_000_000);
        long sleepMillis = Math.min(options.pollMillis(), remainingMillis);
        if (sleepMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleepUntilNextEventPoll(AgentRunEventsFollowOptions options) {
        try {
            Thread.sleep(options.pollMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while following run events.", e);
        }
    }

    private static long elapsedMillis(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }

    private static String messageFor(AgentRunHandle handle) {
        AgentRunHandle normalized = handle == null ? AgentRunHandle.unknown("") : handle;
        return "Run state is " + normalized.state().name().toLowerCase().replace('_', ' ') + ".";
    }

    private static String historyMessage(
            AgentRunHistoryQuery query,
            List<AgentRunStatus> matches,
            List<AgentRunStatus> runs) {
        if (!runs.isEmpty()) {
            return "Recorded run statuses.";
        }
        if (!matches.isEmpty()) {
            return "No run statuses are recorded for this page.";
        }
        return query.filtered() ? "No run statuses match the query." : "No run statuses are recorded.";
    }

}

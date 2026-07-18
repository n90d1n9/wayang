package tech.kayys.wayang.agent.store;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tech.kayys.wayang.agent.event.AgentRunEvent;
import tech.kayys.wayang.agent.run.AgentRunStatus;

/**
 * Snapshot compaction rules for bounded file-backed run stores.
 */
final public class AgentRunStoreRetention {

    private AgentRunStoreRetention() {
    }

    public static AgentRunStoreSnapshot apply(
            AgentRunStoreSnapshot snapshot,
            AgentRunStoreRetentionPolicy policy) {
        return plan(snapshot, policy).snapshot();
    }

    public static AgentRunStoreRetentionAssessment assess(
            AgentRunStoreSnapshot snapshot,
            AgentRunStoreRetentionPolicy policy) {
        return plan(snapshot, policy).assessment();
    }

    private static RetentionPlan plan(
            AgentRunStoreSnapshot snapshot,
            AgentRunStoreRetentionPolicy policy) {
        AgentRunStoreSnapshot normalizedSnapshot = snapshot == null
                ? AgentRunStoreSnapshot.empty()
                : snapshot;
        AgentRunStoreRetentionPolicy normalizedPolicy = policy == null
                ? AgentRunStoreRetentionPolicy.defaults()
                : policy;
        Set<String> allRunIds = runIds(normalizedSnapshot.statuses(), normalizedSnapshot.events());
        Set<String> retainedRunIds = retainedRunIds(
                normalizedSnapshot.statuses(),
                normalizedSnapshot.events(),
                normalizedPolicy);
        List<AgentRunStatus> statuses = retainedStatuses(
                normalizedSnapshot.statuses(),
                retainedRunIds);
        Set<Integer> retainedEventIndexes = retainedEventIndexes(
                normalizedSnapshot.events(),
                retainedRunIds,
                normalizedPolicy);
        List<AgentRunEvent> events = retainedEvents(
                retainedRunIds,
                normalizedSnapshot.events(),
                retainedEventIndexes);
        AgentRunStoreSnapshot retained = new AgentRunStoreSnapshot(statuses, events);
        AgentRunStoreRetentionAssessment assessment = assessment(
                normalizedSnapshot,
                retained,
                normalizedPolicy,
                allRunIds,
                retainedRunIds,
                retainedEventIndexes);
        return new RetentionPlan(retained, assessment);
    }

    private static Set<String> retainedRunIds(
            List<AgentRunStatus> statuses,
            List<AgentRunEvent> events,
            AgentRunStoreRetentionPolicy policy) {
        Set<String> runIds = runIds(statuses, events);
        if (!policy.limitsRuns() || runIds.size() <= policy.maxRuns()) {
            return runIds;
        }
        Map<String, Integer> lastEventIndexByRun = lastEventIndexByRun(events);
        Map<String, Integer> originalIndexByRun = originalIndexByRun(statuses, events);
        return runIds.stream()
                .sorted(Comparator
                        .comparingInt((String runId) -> lastEventIndexByRun.getOrDefault(runId, -1))
                        .thenComparingInt(originalIndexByRun::get))
                .skip(runIds.size() - policy.maxRuns())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private static List<AgentRunStatus> retainedStatuses(
            List<AgentRunStatus> statuses,
            Set<String> retainedRunIds) {
        return statuses.stream()
                .filter(status -> retainedRunIds.contains(status.handle().runId()))
                .toList();
    }

    private static List<AgentRunEvent> retainedEvents(
            Set<String> retainedRunIds,
            List<AgentRunEvent> events,
            Set<Integer> retainedEventIndexes) {
        List<AgentRunEvent> retained = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            AgentRunEvent event = events.get(i);
            if (retainedRunIds.contains(event.runId()) && retainedEventIndexes.contains(i)) {
                retained.add(event);
            }
        }
        return retained;
    }

    private static AgentRunStoreRetentionAssessment assessment(
            AgentRunStoreSnapshot original,
            AgentRunStoreSnapshot retained,
            AgentRunStoreRetentionPolicy policy,
            Set<String> allRunIds,
            Set<String> retainedRunIds,
            Set<Integer> retainedEventIndexes) {
        List<String> retainedRunIdList = retainedRunIds.stream().toList();
        List<String> prunedRunIds = allRunIds.stream()
                .filter(runId -> !retainedRunIds.contains(runId))
                .toList();
        return new AgentRunStoreRetentionAssessment(
                policy,
                allRunIds.size(),
                retainedRunIds.size(),
                prunedRunIds.size(),
                original.statuses().size(),
                retained.statuses().size(),
                original.statuses().size() - retained.statuses().size(),
                original.events().size(),
                retained.events().size(),
                original.events().size() - retained.events().size(),
                retainedRunIdList,
                prunedRunIds,
                prunedEventsByRun(original.events(), retainedRunIds, retainedEventIndexes));
    }

    private static Map<String, Integer> prunedEventsByRun(
            List<AgentRunEvent> events,
            Set<String> retainedRunIds,
            Set<Integer> retainedEventIndexes) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < events.size(); i++) {
            AgentRunEvent event = events.get(i);
            if (!retainedRunIds.contains(event.runId()) || !retainedEventIndexes.contains(i)) {
                counts.merge(event.runId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static Set<Integer> retainedEventIndexes(
            List<AgentRunEvent> events,
            Set<String> retainedRunIds,
            AgentRunStoreRetentionPolicy policy) {
        if (!policy.limitsEventsPerRun()) {
            Set<Integer> all = new LinkedHashSet<>();
            for (int i = 0; i < events.size(); i++) {
                all.add(i);
            }
            return all;
        }
        Map<String, List<IndexedEvent>> byRun = indexedEventsByRun(events, retainedRunIds);
        Set<Integer> retainedIndexes = new LinkedHashSet<>();
        for (List<IndexedEvent> runEvents : byRun.values()) {
            List<IndexedEvent> ordered = runEvents.stream()
                    .sorted(Comparator.comparingLong(indexed -> indexed.event().sequence()))
                    .toList();
            int offset = Math.max(0, ordered.size() - policy.maxEventsPerRun());
            ordered.subList(offset, ordered.size())
                    .forEach(indexed -> retainedIndexes.add(indexed.index()));
        }
        return retainedIndexes;
    }

    private static Map<String, Integer> lastEventIndexByRun(List<AgentRunEvent> events) {
        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (int i = 0; i < events.size(); i++) {
            indexes.put(events.get(i).runId(), i);
        }
        return indexes;
    }

    private static Map<String, Integer> originalIndexByRun(
            List<AgentRunStatus> statuses,
            List<AgentRunEvent> events) {
        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (int i = 0; i < statuses.size(); i++) {
            indexes.put(statuses.get(i).handle().runId(), i);
        }
        int offset = statuses.size();
        for (int i = 0; i < events.size(); i++) {
            indexes.putIfAbsent(events.get(i).runId(), offset + i);
        }
        return indexes;
    }

    private static Set<String> runIds(List<AgentRunStatus> statuses, List<AgentRunEvent> events) {
        Set<String> runIds = new LinkedHashSet<>();
        for (AgentRunStatus status : statuses) {
            runIds.add(status.handle().runId());
        }
        for (AgentRunEvent event : events) {
            runIds.add(event.runId());
        }
        return runIds;
    }

    private static Map<String, List<IndexedEvent>> indexedEventsByRun(
            List<AgentRunEvent> events,
            Set<String> retainedRunIds) {
        Map<String, List<IndexedEvent>> byRun = new LinkedHashMap<>();
        for (int i = 0; i < events.size(); i++) {
            AgentRunEvent event = events.get(i);
            if (retainedRunIds.contains(event.runId())) {
                byRun.computeIfAbsent(event.runId(), ignored -> new ArrayList<>())
                        .add(new IndexedEvent(i, event));
            }
        }
        return byRun;
    }

    private record IndexedEvent(int index, AgentRunEvent event) {
    }

    private record RetentionPlan(
            AgentRunStoreSnapshot snapshot,
            AgentRunStoreRetentionAssessment assessment) {
    }
}

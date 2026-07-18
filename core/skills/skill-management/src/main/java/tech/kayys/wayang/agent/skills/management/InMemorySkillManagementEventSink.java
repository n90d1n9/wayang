package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

/**
 * In-memory event sink for tests and lightweight local diagnostics.
 */
public final class InMemorySkillManagementEventSink
        implements SkillManagementEventSink, SkillManagementEventReader, SkillManagementEventPruner {

    public static final int DEFAULT_MAX_EVENTS = 10_000;

    private final int maxEvents;
    private final CopyOnWriteArrayList<SkillManagementEvent> events = new CopyOnWriteArrayList<>();

    public InMemorySkillManagementEventSink() {
        this(DEFAULT_MAX_EVENTS);
    }

    public InMemorySkillManagementEventSink(int maxEvents) {
        this.maxEvents = SkillManagementEventRetention.normalizeCapacity(maxEvents);
    }

    @Override
    public synchronized void record(SkillManagementEvent event) {
        if (event != null) {
            events.add(event);
            trimToCapacity();
        }
    }

    public synchronized List<SkillManagementEvent> events() {
        return List.copyOf(events);
    }

    @Override
    public synchronized SkillManagementEventPage query(SkillManagementEventQuery query) {
        return SkillManagementEventPages.from(events, query);
    }

    public synchronized void clear() {
        events.clear();
    }

    @Override
    public synchronized SkillManagementEventPruneResult pruneEvents(SkillManagementEventPruneOptions options) {
        SkillManagementEventPruneOptions resolved =
                SkillManagementEventRetention.resolve(options, maxEvents);
        List<SkillManagementEvent> snapshot = events();
        List<SkillManagementEvent> targets =
                SkillManagementEventRetention.oldestToPrune(snapshot, resolved.keepLatestEvents());
        List<String> references = IntStream.range(0, targets.size())
                .mapToObj(index -> reference(targets.get(index), index))
                .toList();
        if (!resolved.dryRun()) {
            for (int index = 0; index < targets.size(); index++) {
                events.remove(0);
            }
        }
        return SkillManagementEventPruneResult.success(resolved, snapshot.size(), references);
    }

    private void trimToCapacity() {
        int removable = SkillManagementEventRetention.removableCount(events.size(), maxEvents);
        for (int index = 0; index < removable; index++) {
            events.remove(0);
        }
    }

    private String reference(SkillManagementEvent event, int index) {
        return String.format(
                java.util.Locale.ROOT,
                "%020d-%04d-%s-%s",
                event.occurredAt().toEpochMilli(),
                index,
                event.operation().name(),
                event.skillId());
    }
}

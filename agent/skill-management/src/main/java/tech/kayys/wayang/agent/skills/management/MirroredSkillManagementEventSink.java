package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Event sink that reads as primary/fallback and records each event to both.
 */
public final class MirroredSkillManagementEventSink
        implements SkillManagementEventSink, SkillManagementEventReader, SkillManagementEventPruner {

    private final SkillManagementEventSink primary;
    private final SkillManagementEventSink fallback;

    public MirroredSkillManagementEventSink(
            SkillManagementEventSink primary,
            SkillManagementEventSink fallback) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    public SkillManagementEventSink primary() {
        return primary;
    }

    public SkillManagementEventSink fallback() {
        return fallback;
    }

    @Override
    public void record(SkillManagementEvent event) {
        if (event == null) {
            return;
        }
        recordSafely(primary, event);
        recordSafely(fallback, event);
    }

    @Override
    public SkillManagementEventPage query(SkillManagementEventQuery query) {
        return querySafely(primary, query)
                .or(() -> querySafely(fallback, query))
                .orElseGet(() -> SkillManagementEventReader.empty().query(query));
    }

    @Override
    public SkillManagementEventPruneResult pruneEvents(SkillManagementEventPruneOptions options) {
        return SkillManagementEventPruneSupport.pruneChildren(
                options,
                List.of(
                        primary,
                        fallback),
                "Event history pruning is not supported for mirrored event sink");
    }

    @Override
    public boolean supportsPruning() {
        return SkillManagementEventPruneSupport.supportsPruning(List.of(primary, fallback));
    }

    private static void recordSafely(SkillManagementEventSink sink, SkillManagementEvent event) {
        try {
            sink.record(event);
        } catch (RuntimeException ignored) {
            // Event history must stay observability-only, even in mirrored mode.
        }
    }

    private static Optional<SkillManagementEventPage> querySafely(
            SkillManagementEventSink sink,
            SkillManagementEventQuery query) {
        try {
            return SkillManagementEventReader.readableSink(sink)
                    .map(reader -> reader.query(query));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }
}

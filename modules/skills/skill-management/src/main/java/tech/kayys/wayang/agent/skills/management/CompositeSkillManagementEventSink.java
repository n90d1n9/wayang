package tech.kayys.wayang.agent.skills.management;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Best-effort event sink that fans out each event to multiple downstream sinks.
 */
public final class CompositeSkillManagementEventSink
        implements SkillManagementEventSink, SkillManagementEventReader, SkillManagementEventPruner {

    private final List<SkillManagementEventSink> sinks;

    public CompositeSkillManagementEventSink(List<? extends SkillManagementEventSink> sinks) {
        if (sinks == null) {
            this.sinks = List.of();
        } else {
            this.sinks = sinks.stream()
                    .filter(Objects::nonNull)
                    .map(SkillManagementEventSink.class::cast)
                    .toList();
        }
    }

    public CompositeSkillManagementEventSink(SkillManagementEventSink... sinks) {
        this(sinks == null ? List.of() : Arrays.asList(sinks));
    }

    public List<SkillManagementEventSink> sinks() {
        return sinks;
    }

    @Override
    public void record(SkillManagementEvent event) {
        if (event == null) {
            return;
        }
        for (SkillManagementEventSink sink : sinks) {
            try {
                sink.record(event);
            } catch (RuntimeException ignored) {
                // A failing diagnostic target must not block other event sinks.
            }
        }
    }

    @Override
    public SkillManagementEventPage query(SkillManagementEventQuery query) {
        for (SkillManagementEventSink sink : sinks) {
            Optional<SkillManagementEventReader> reader = SkillManagementEventReader.readableSink(sink);
            if (reader.isPresent()) {
                try {
                    return reader.get().query(query);
                } catch (RuntimeException ignored) {
                    // A failing reader must not hide later readable event stores.
                }
            }
        }
        return SkillManagementEventReader.empty().query(query);
    }

    @Override
    public SkillManagementEventPruneResult pruneEvents(SkillManagementEventPruneOptions options) {
        return SkillManagementEventPruneSupport.pruneChildren(
                options,
                sinks,
                "Event history pruning is not supported for composite event sink");
    }

    @Override
    public boolean supportsPruning() {
        return SkillManagementEventPruneSupport.supportsPruning(sinks);
    }
}

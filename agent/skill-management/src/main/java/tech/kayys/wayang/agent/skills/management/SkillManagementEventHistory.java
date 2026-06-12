package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Read and maintenance access to skill-management event history.
 */
final class SkillManagementEventHistory {

    private final SkillManagementEventReader eventReader;
    private final SkillManagementEventPruner eventPruner;

    SkillManagementEventHistory(
            SkillManagementEventReader eventReader,
            SkillManagementEventPruner eventPruner) {
        this.eventReader = Objects.requireNonNull(eventReader, "eventReader");
        this.eventPruner = Objects.requireNonNull(eventPruner, "eventPruner");
    }

    SkillManagementEventPage query(SkillManagementEventQuery query) {
        return eventReader.query(query);
    }

    SkillManagementEventPage latest() {
        return eventReader.latest();
    }

    SkillManagementEventPruneResult prune(SkillManagementEventPruneOptions options) {
        return eventPruner.pruneEvents(options);
    }

    boolean supportsPruning() {
        return eventPruner.supportsPruning();
    }
}

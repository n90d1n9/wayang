package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable in-memory view of one persisted run-store snapshot, keeping status
 * and timeline event lists from the same properties read together.
 */
record AgentRunStoreSnapshot(List<AgentRunStatus> statuses, List<AgentRunEvent> events) {

    AgentRunStoreSnapshot {
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
        events = events == null ? List.of() : List.copyOf(events);
    }

    static AgentRunStoreSnapshot empty() {
        return new AgentRunStoreSnapshot(List.of(), List.of());
    }

    List<AgentRunStatus> mutableStatuses() {
        return new ArrayList<>(statuses);
    }

    List<AgentRunEvent> mutableEvents() {
        return new ArrayList<>(events);
    }
}

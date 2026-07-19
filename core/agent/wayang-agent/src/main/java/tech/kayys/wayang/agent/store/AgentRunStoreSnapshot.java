package tech.kayys.wayang.agent.store;

import java.util.ArrayList;
import java.util.List;

import tech.kayys.wayang.agent.event.AgentRunEvent;
import tech.kayys.wayang.agent.run.AgentRunStatus;

/**
 * Immutable in-memory view of one persisted run-store snapshot, keeping status
 * and timeline event lists from the same properties read together.
 */
public record AgentRunStoreSnapshot(List<AgentRunStatus> statuses, List<AgentRunEvent> events) {

    public AgentRunStoreSnapshot {
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
        events = events == null ? List.of() : List.copyOf(events);
    }

   public  static AgentRunStoreSnapshot empty() {
        return new AgentRunStoreSnapshot(List.of(), List.of());
    }

    List<AgentRunStatus> mutableStatuses() {
        return new ArrayList<>(statuses);
    }

    List<AgentRunEvent> mutableEvents() {
        return new ArrayList<>(events);
    }
}

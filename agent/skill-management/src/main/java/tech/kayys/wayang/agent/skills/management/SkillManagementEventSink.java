package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Receives skill-management operation events for audit, metrics, or logs.
 */
@FunctionalInterface
public interface SkillManagementEventSink {

    void record(SkillManagementEvent event);

    static SkillManagementEventSink noop() {
        return event -> {
        };
    }

    static SkillManagementEventSink composite(List<? extends SkillManagementEventSink> sinks) {
        return new CompositeSkillManagementEventSink(sinks);
    }

    static SkillManagementEventSink composite(SkillManagementEventSink... sinks) {
        return new CompositeSkillManagementEventSink(sinks);
    }
}

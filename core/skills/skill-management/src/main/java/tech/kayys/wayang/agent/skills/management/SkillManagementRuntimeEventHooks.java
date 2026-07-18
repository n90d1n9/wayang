package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Event-facing runtime components shared by readers, runners, and workflows.
 */
record SkillManagementRuntimeEventHooks(
        SkillManagementEventSink eventSink,
        SkillManagementEventReader eventReader,
        SkillManagementEventRecorder eventRecorder,
        SkillManagementEventPruner eventPruner,
        SkillManagementEventHistory eventHistory,
        SkillManagementOperationTraceReader operationTraceReader) {

    SkillManagementRuntimeEventHooks {
        eventSink = Objects.requireNonNull(eventSink, "eventSink");
        eventReader = Objects.requireNonNull(eventReader, "eventReader");
        eventRecorder = Objects.requireNonNull(eventRecorder, "eventRecorder");
        eventPruner = Objects.requireNonNull(eventPruner, "eventPruner");
        eventHistory = Objects.requireNonNull(eventHistory, "eventHistory");
        operationTraceReader = Objects.requireNonNull(operationTraceReader, "operationTraceReader");
    }

    static SkillManagementRuntimeEventHooks of(
            SkillManagementEventSink eventSink,
            SkillManagementEventReader eventReader) {
        SkillManagementEventSink resolvedSink =
                Objects.requireNonNullElseGet(eventSink, SkillManagementEventSink::noop);
        SkillManagementEventReader resolvedReader =
                Objects.requireNonNullElseGet(eventReader, SkillManagementEventReader::empty);
        SkillManagementEventRecorder recorder = new SkillManagementEventRecorder(resolvedSink);
        SkillManagementEventPruner pruner = SkillManagementEventPruner.forSink(resolvedSink);
        SkillManagementEventHistory history = new SkillManagementEventHistory(resolvedReader, pruner);
        return new SkillManagementRuntimeEventHooks(
                resolvedSink,
                resolvedReader,
                recorder,
                pruner,
                history,
                new SkillManagementOperationTraceReader(history));
    }
}

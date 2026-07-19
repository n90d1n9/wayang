package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Shapes definition mutation write failures into events and service exceptions.
 */
final class SkillManagementDefinitionWriteFailure {

    private final SkillManagementEventRecorder eventRecorder;

    SkillManagementDefinitionWriteFailure(SkillManagementEventRecorder eventRecorder) {
        this.eventRecorder = Objects.requireNonNull(eventRecorder, "eventRecorder");
    }

    SkillManagementWriteException record(
            SkillManagementEventOperation eventOperation,
            String operationName,
            String skillId,
            RuntimeException error,
            SkillManagementOperationContext context) {
        eventRecorder.failure(eventOperation, skillId, error, context);
        return new SkillManagementWriteException(
                "Failed to " + operationName + " skill consistently: " + skillId,
                error);
    }
}

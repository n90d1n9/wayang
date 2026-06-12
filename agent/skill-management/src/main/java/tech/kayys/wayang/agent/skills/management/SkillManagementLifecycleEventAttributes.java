package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.*;

/**
 * Event-attribute projection for lifecycle transitions, repair, and bootstrap readiness.
 */
final class SkillManagementLifecycleEventAttributes {

    private SkillManagementLifecycleEventAttributes() {
    }

    static Map<String, String> transition(SkillLifecycleStatus status) {
        Objects.requireNonNull(status, "status");
        return Map.of(STATUS, status.name());
    }

    static Map<String, String> transition(SkillLifecycleState state) {
        Objects.requireNonNull(state, "state");
        return Map.of(
                STATUS, state.status().name(),
                REVISION, String.valueOf(state.revision()));
    }

    static Map<String, String> revision(SkillLifecycleState state) {
        Objects.requireNonNull(state, "state");
        return Map.of(REVISION, String.valueOf(state.revision()));
    }

    static Map<String, String> reconcile(SkillLifecycleStateReconcileResult result) {
        Objects.requireNonNull(result, "result");
        return Map.of(
                CONSISTENT, String.valueOf(result.consistent()),
                CREATED, String.valueOf(result.createdStateSkillIds().size()),
                REMOVED, String.valueOf(result.removedStateSkillIds().size()),
                MISSING, String.valueOf(result.missingStateSkillIds().size()),
                ORPHANED, String.valueOf(result.orphanedStateSkillIds().size()));
    }

    static Map<String, String> bootstrap(SkillManagementBootstrapResult result) {
        Objects.requireNonNull(result, "result");
        return Map.of(
                READY, String.valueOf(result.ready()),
                CHANGED, String.valueOf(result.lifecycleStateChanged()),
                LIFECYCLE_CONSISTENT, String.valueOf(result.lifecycleStateConsistent()));
    }
}

package tech.kayys.wayang.agent.skills.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Runs lifecycle-state transitions and reconciliation with event recording.
 */
final class SkillManagementLifecycleRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SkillManagementLifecycleRunner.class);

    private final SkillDefinitionStore definitionStore;
    private final SkillLifecycleStateStore lifecycleStateStore;
    private final SkillLifecycleStateResolver lifecycleStateResolver;
    private final SkillLifecycleStateReconciler lifecycleStateReconciler;
    private final SkillManagementEventRecorder eventRecorder;

    SkillManagementLifecycleRunner(
            SkillDefinitionStore definitionStore,
            SkillLifecycleStateStore lifecycleStateStore,
            SkillLifecycleStateReconciler lifecycleStateReconciler,
            SkillManagementEventRecorder eventRecorder) {
        this.definitionStore = Objects.requireNonNull(definitionStore, "definitionStore");
        this.lifecycleStateStore = Objects.requireNonNull(lifecycleStateStore, "lifecycleStateStore");
        this.lifecycleStateResolver = new SkillLifecycleStateResolver(lifecycleStateStore);
        this.lifecycleStateReconciler =
                Objects.requireNonNull(lifecycleStateReconciler, "lifecycleStateReconciler");
        this.eventRecorder = Objects.requireNonNull(eventRecorder, "eventRecorder");
    }

    SkillLifecycleState transition(String skillId, SkillLifecycleStatus status) {
        if (definitionStore.getSkill(skillId).isEmpty()) {
            throw new IllegalStateException("Skill not found: " + skillId);
        }
        SkillManagementOperationContext context = SkillManagementOperationContext.root();
        SkillLifecycleState next = nextStatus(skillId, status);
        return eventRecorder.recordOperation(
                SkillManagementEventOperation.TRANSITION_SKILL,
                skillId,
                context,
                () -> {
                    lifecycleStateStore.save(next);
                    LOG.info("Skill {} moved to {}", skillId, status);
                    return next;
                },
                SkillManagementEventAttributes::lifecycleTransition,
                error -> SkillManagementEventAttributes.lifecycleTransition(status));
    }

    SkillLifecycleState stateForExisting(String skillId) {
        if (definitionStore.getSkill(skillId).isEmpty()) {
            throw new IllegalStateException("Skill not found: " + skillId);
        }
        return viewStateFor(skillId);
    }

    SkillLifecycleState viewStateFor(String skillId) {
        return lifecycleStateResolver.view(skillId);
    }

    Map<String, SkillLifecycleState> snapshot() {
        return lifecycleStateStore.snapshot();
    }

    SkillLifecycleStateReconcileResult reconcile(SkillLifecycleStateReconcileOptions options) {
        SkillManagementOperationContext context = SkillManagementOperationContext.root();
        return eventRecorder.recordOperation(
                SkillManagementEventOperation.RECONCILE_LIFECYCLE,
                "",
                context,
                () -> lifecycleStateReconciler.reconcile(definitionStore, lifecycleStateStore, options),
                SkillManagementEventAttributes::lifecycleReconcile);
    }

    private SkillLifecycleState nextStatus(String skillId, SkillLifecycleStatus status) {
        return lifecycleStateStore.get(skillId)
                .orElseGet(() -> SkillLifecycleState.created(skillId))
                .withStatus(status);
    }
}

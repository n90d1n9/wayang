package tech.kayys.wayang.agent.skills.management;

import java.util.Map;

/**
 * Shared event-attribute projection for skill-management operations.
 */
final class SkillManagementEventAttributes {

    private SkillManagementEventAttributes() {
    }

    static Map<String, String> artifactSync(SkillArtifactStoreSyncResult result) {
        return SkillManagementArtifactEventAttributes.sync(result);
    }

    static Map<String, String> artifact(SkillArtifact artifact) {
        return SkillManagementArtifactEventAttributes.artifact(artifact);
    }

    static Map<String, String> artifact(SkillArtifactReference reference) {
        return SkillManagementArtifactEventAttributes.reference(reference);
    }

    static Map<String, String> artifactDeleted(
            SkillArtifactReference reference,
            boolean deleted) {
        return SkillManagementArtifactEventAttributes.deleted(reference, deleted);
    }

    static Map<String, String> artifact(
            SkillArtifactReference reference,
            Map<String, String> extra) {
        return SkillManagementArtifactEventAttributes.reference(reference, extra);
    }

    static Map<String, String> lifecycleTransition(SkillLifecycleStatus status) {
        return SkillManagementLifecycleEventAttributes.transition(status);
    }

    static Map<String, String> lifecycleTransition(SkillLifecycleState state) {
        return SkillManagementLifecycleEventAttributes.transition(state);
    }

    static Map<String, String> skillRevision(SkillLifecycleState state) {
        return SkillManagementLifecycleEventAttributes.revision(state);
    }

    static Map<String, String> lifecycleReconcile(SkillLifecycleStateReconcileResult result) {
        return SkillManagementLifecycleEventAttributes.reconcile(result);
    }

    static Map<String, String> bootstrap(SkillManagementBootstrapResult result) {
        return SkillManagementLifecycleEventAttributes.bootstrap(result);
    }

    static Map<String, String> maintenance(
            SkillManagementMaintenanceResult result,
            SkillManagementMaintenancePlan plan) {
        return SkillManagementMaintenanceEventAttributes.maintenance(result, plan);
    }

    static Map<String, String> maintenance(
            SkillManagementMaintenanceResult result,
            SkillManagementMaintenancePlan plan,
            SkillManagementOperationContext context) {
        return SkillManagementMaintenanceEventAttributes.maintenance(result, plan, context);
    }

    static Map<String, String> deployment(SkillManagementDeploymentResult result) {
        return SkillManagementMaintenanceEventAttributes.deployment(result);
    }

    static Map<String, String> deployment(
            SkillManagementDeploymentResult result,
            SkillManagementOperationContext context) {
        return SkillManagementMaintenanceEventAttributes.deployment(result, context);
    }

    static Map<String, String> preflight(SkillManagementDeploymentPreflightReport report) {
        return SkillManagementPreflightEventAttributes.deployment(report);
    }

    static Map<String, String> preflight(SkillManagementPreflightReport report) {
        return SkillManagementPreflightEventAttributes.validation(report);
    }

    static Map<String, String> failure(RuntimeException error, Map<String, String> attributes) {
        return SkillManagementFailureEventAttributes.failure(error, attributes);
    }

    static Map<String, String> withContext(
            Map<String, String> attributes,
            SkillManagementOperationContext context) {
        return SkillManagementFailureEventAttributes.withContext(attributes, context);
    }

    static Map<String, String> failure(
            RuntimeException error,
            Map<String, String> attributes,
            SkillManagementOperationContext context) {
        return SkillManagementFailureEventAttributes.failure(error, attributes, context);
    }
}

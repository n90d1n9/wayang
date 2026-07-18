package tech.kayys.wayang.agent.skills.management;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.*;

/**
 * Event-attribute projection for maintenance and deployment run summaries.
 */
final class SkillManagementMaintenanceEventAttributes {

    private SkillManagementMaintenanceEventAttributes() {
    }

    static Map<String, String> maintenance(
            SkillManagementMaintenanceResult result,
            SkillManagementMaintenancePlan plan) {
        return maintenance(result, plan, null);
    }

    static Map<String, String> maintenance(
            SkillManagementMaintenanceResult result,
            SkillManagementMaintenancePlan plan,
            SkillManagementOperationContext context) {
        Objects.requireNonNull(result, "result");
        SkillManagementMaintenancePlan resolvedPlan =
                SkillManagementConfigResolution.maintenancePlan(plan);
        SkillManagementMaintenanceSummary summary = result.summary();
        LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
        attributes.put(DRY_RUN, String.valueOf(summary.dryRun()));
        attributes.put(CHANGED, String.valueOf(summary.changed()));
        attributes.put(CONSISTENT, String.valueOf(summary.consistent()));
        attributes.put(DEFINITION_CHANGED, String.valueOf(summary.definitionChanged()));
        attributes.put(DEFINITION_CHANGES, String.valueOf(summary.definitionChanges()));
        attributes.put(ARTIFACT_CHANGED, String.valueOf(summary.artifactChanged()));
        attributes.put(ARTIFACT_CHANGES, String.valueOf(summary.artifactChanges()));
        attributes.put(ARTIFACT_CONFLICTS, String.valueOf(summary.artifactConflicts()));
        attributes.put(LIFECYCLE_CREATED, String.valueOf(summary.lifecycleCreated()));
        attributes.put(LIFECYCLE_REMOVED, String.valueOf(summary.lifecycleRemoved()));
        attributes.put(EVENT_PRUNE_ENABLED, String.valueOf(resolvedPlan.eventPrunePolicy().enabled()));
        attributes.put(EVENT_PRUNE_SKIPPED, String.valueOf(summary.eventPruneSkipped()));
        attributes.put(EVENT_PRUNE_CHANGED, String.valueOf(summary.eventPruneChanged()));
        attributes.put(EVENT_PRUNED, String.valueOf(summary.eventPruned()));
        SkillManagementMaintenanceStepEventAttributes.put(attributes, result.stepDiagnostics());
        putContext(attributes, context);
        return Map.copyOf(attributes);
    }

    static Map<String, String> deployment(SkillManagementDeploymentResult result) {
        return deployment(result, null);
    }

    static Map<String, String> deployment(
            SkillManagementDeploymentResult result,
            SkillManagementOperationContext context) {
        Objects.requireNonNull(result, "result");
        return maintenance(result.maintenanceResult(), result.config().maintenancePlan(), context);
    }

    private static void putContext(
            LinkedHashMap<String, String> attributes,
            SkillManagementOperationContext context) {
        if (context != null) {
            attributes.putAll(context.attributes());
        }
    }
}

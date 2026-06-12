package tech.kayys.wayang.agent.skills.management;

import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.*;

/**
 * Deployment history event-prune fields decoded from event attributes.
 */
record SkillManagementAdminDeploymentHistoryEventPruneAttributes(
        boolean enabled,
        boolean skipped,
        boolean changed,
        int pruned) {

    SkillManagementAdminDeploymentHistoryEventPruneAttributes {
        pruned = SkillManagementAdminValueSupport.nonNegative(pruned);
    }

    static SkillManagementAdminDeploymentHistoryEventPruneAttributes empty() {
        return from(null);
    }

    static SkillManagementAdminDeploymentHistoryEventPruneAttributes from(
            SkillManagementEventAttributeReader attributes) {
        SkillManagementEventAttributeReader reader = SkillManagementEventAttributeReader.orEmpty(attributes);
        return new SkillManagementAdminDeploymentHistoryEventPruneAttributes(
                reader.flag(EVENT_PRUNE_ENABLED),
                reader.flag(EVENT_PRUNE_SKIPPED),
                reader.flag(EVENT_PRUNE_CHANGED),
                reader.count(EVENT_PRUNED));
    }
}

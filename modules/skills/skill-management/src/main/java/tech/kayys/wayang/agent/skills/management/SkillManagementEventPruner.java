package tech.kayys.wayang.agent.skills.management;

/**
 * Optional maintenance contract for event-history stores that can prune old events.
 */
public interface SkillManagementEventPruner {

    String PRUNE_EVENTS_CAPABILITY_REQUIRED =
            SkillStoreCapabilityRequirement.eventSinkPruning().errorMessage();

    SkillManagementEventPruneResult pruneEvents(SkillManagementEventPruneOptions options);

    default boolean supportsPruning() {
        return true;
    }

    static SkillManagementEventPruner forSink(SkillManagementEventSink sink) {
        return sink instanceof SkillManagementEventPruner pruner
                ? pruner
                : unsupported(sink);
    }

    static SkillManagementEventPruner unsupported(Object target) {
        String storeType = target == null ? "unknown" : SkillStoreInspectionSupport.storeType(target);
        return new SkillManagementEventPruner() {
            @Override
            public SkillManagementEventPruneResult pruneEvents(SkillManagementEventPruneOptions options) {
                return SkillManagementEventPruneResult.failure(
                        options,
                        "Event history pruning is not supported for " + storeType);
            }

            @Override
            public boolean supportsPruning() {
                return false;
            }
        };
    }
}

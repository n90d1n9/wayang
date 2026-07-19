package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Shared pruning rules for event sinks that fan out to child sinks.
 */
final class SkillManagementEventPruneSupport {

    private SkillManagementEventPruneSupport() {
    }

    static boolean supportsPruning(List<? extends SkillManagementEventSink> sinks) {
        return sinks != null
                && !sinks.isEmpty()
                && sinks.stream()
                        .map(SkillManagementEventPruner::forSink)
                        .allMatch(SkillManagementEventPruner::supportsPruning);
    }

    static SkillManagementEventPruneResult pruneChildren(
            SkillManagementEventPruneOptions options,
            List<? extends SkillManagementEventSink> sinks,
            String emptyFailure) {
        List<? extends SkillManagementEventSink> resolvedSinks = sinks == null ? List.of() : sinks;
        if (resolvedSinks.isEmpty()) {
            return SkillManagementEventPruneResult.failure(options, emptyFailure);
        }
        if (!supportsPruning(resolvedSinks)) {
            return SkillManagementEventPruneResult.composite(
                    options,
                    resolvedSinks.stream()
                            .map(sink -> {
                                SkillManagementEventPruner pruner = SkillManagementEventPruner.forSink(sink);
                                return pruner.supportsPruning()
                                        ? SkillManagementEventPruneResult.skipped(options)
                                        : pruner.pruneEvents(options);
                            })
                            .toList());
        }
        return SkillManagementEventPruneResult.composite(
                options,
                resolvedSinks.stream()
                        .map(sink -> SkillManagementEventPruner.forSink(sink).pruneEvents(options))
                        .toList());
    }
}

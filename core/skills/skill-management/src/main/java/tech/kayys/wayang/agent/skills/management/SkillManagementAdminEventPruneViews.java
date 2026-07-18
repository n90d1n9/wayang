package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Maps event-history prune results to stable admin DTOs.
 */
final class SkillManagementAdminEventPruneViews {

    private SkillManagementAdminEventPruneViews() {
    }

    static SkillManagementAdminEventPruneReport eventPrune(SkillManagementEventPruneResult result) {
        Objects.requireNonNull(result, "result");
        return new SkillManagementAdminEventPruneReport(
                result.dryRun(),
                result.skipped(),
                result.keepLatestEvents(),
                result.scannedEvents(),
                result.prunedEventReferences(),
                result.failure(),
                result.children().stream()
                        .map(SkillManagementAdminEventPruneViews::eventPrune)
                        .toList());
    }
}

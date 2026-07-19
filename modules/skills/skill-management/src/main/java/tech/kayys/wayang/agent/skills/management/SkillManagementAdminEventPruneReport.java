package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Stable admin-facing projection of an event-history prune operation.
 */
public record SkillManagementAdminEventPruneReport(
        boolean dryRun,
        boolean skipped,
        boolean success,
        boolean changed,
        int keepLatestEvents,
        int scannedEvents,
        int prunedEvents,
        List<String> prunedEventReferences,
        String failure,
        List<SkillManagementAdminEventPruneReport> children) {

    public SkillManagementAdminEventPruneReport(
            boolean dryRun,
            boolean skipped,
            int keepLatestEvents,
            int scannedEvents,
            List<String> prunedEventReferences,
            String failure,
            List<SkillManagementAdminEventPruneReport> children) {
        this(
                dryRun,
                skipped,
                false,
                false,
                keepLatestEvents,
                scannedEvents,
                0,
                prunedEventReferences,
                failure,
                children);
    }

    public SkillManagementAdminEventPruneReport {
        keepLatestEvents = SkillManagementAdminValueSupport.nonNegative(keepLatestEvents);
        prunedEventReferences = SkillManagementAdminValueSupport.compactStrings(prunedEventReferences);
        prunedEvents = prunedEventReferences.size();
        scannedEvents = SkillManagementAdminValueSupport.atLeast(scannedEvents, prunedEvents);
        failure = SkillManagementAdminValueSupport.blankToEmpty(failure);
        children = SkillManagementAdminValueSupport.nonNullList(children);
        success = failure.isBlank() && children.stream().allMatch(SkillManagementAdminEventPruneReport::success);
        changed = success && !skipped && !dryRun && prunedEvents > 0;
    }
}

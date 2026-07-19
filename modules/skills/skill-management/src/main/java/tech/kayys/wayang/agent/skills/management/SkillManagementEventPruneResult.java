package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Result of previewing or applying skill-management event-history pruning.
 */
public record SkillManagementEventPruneResult(
        boolean dryRun,
        boolean skipped,
        int keepLatestEvents,
        int scannedEvents,
        int prunedEvents,
        List<String> prunedEventReferences,
        String failure,
        List<SkillManagementEventPruneResult> children) {

    public SkillManagementEventPruneResult(
            boolean dryRun,
            int keepLatestEvents,
            int scannedEvents,
            int prunedEvents,
            List<String> prunedEventReferences,
            String failure,
            List<SkillManagementEventPruneResult> children) {
        this(
                dryRun,
                false,
                keepLatestEvents,
                scannedEvents,
                prunedEvents,
                prunedEventReferences,
                failure,
                children);
    }

    public SkillManagementEventPruneResult {
        keepLatestEvents = SkillManagementValueSupport.nonNegative(keepLatestEvents);
        prunedEventReferences = SkillManagementValueSupport.compactStrings(prunedEventReferences);
        prunedEvents = SkillManagementValueSupport.atLeast(prunedEvents, prunedEventReferences.size());
        scannedEvents = SkillManagementValueSupport.atLeast(scannedEvents, prunedEvents);
        failure = SkillManagementValueSupport.blankToEmpty(failure);
        children = SkillManagementValueSupport.nonNullList(children);
    }

    public static SkillManagementEventPruneResult success(
            SkillManagementEventPruneOptions options,
            int scannedEvents,
            List<String> prunedEventReferences) {
        SkillManagementEventPruneOptions resolved = options == null
                ? SkillManagementEventPruneOptions.keepLatest(InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS)
                : options;
        return new SkillManagementEventPruneResult(
                resolved.dryRun(),
                resolved.keepLatestEvents(),
                scannedEvents,
                prunedEventReferences == null ? 0 : prunedEventReferences.size(),
                prunedEventReferences,
                "",
                List.of());
    }

    public static SkillManagementEventPruneResult skippedResult() {
        return skipped(SkillManagementEventPruneOptions.keepLatest(InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS));
    }

    public static SkillManagementEventPruneResult skipped(SkillManagementEventPruneOptions options) {
        SkillManagementEventPruneOptions resolved = options == null
                ? SkillManagementEventPruneOptions.keepLatest(InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS)
                : options;
        return new SkillManagementEventPruneResult(
                resolved.dryRun(),
                true,
                resolved.keepLatestEvents(),
                0,
                0,
                List.of(),
                "",
                List.of());
    }

    public static SkillManagementEventPruneResult failure(
            SkillManagementEventPruneOptions options,
            String failure) {
        SkillManagementEventPruneOptions resolved = options == null
                ? SkillManagementEventPruneOptions.keepLatest(InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS)
                : options;
        return new SkillManagementEventPruneResult(
                resolved.dryRun(),
                resolved.keepLatestEvents(),
                0,
                0,
                List.of(),
                failure,
                List.of());
    }

    public static SkillManagementEventPruneResult composite(
            SkillManagementEventPruneOptions options,
            List<SkillManagementEventPruneResult> children) {
        SkillManagementEventPruneOptions resolved = options == null
                ? SkillManagementEventPruneOptions.keepLatest(InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS)
                : options;
        List<SkillManagementEventPruneResult> childResults = SkillManagementValueSupport.nonNullList(children);
        return new SkillManagementEventPruneResult(
                resolved.dryRun(),
                resolved.keepLatestEvents(),
                childResults.stream().mapToInt(SkillManagementEventPruneResult::scannedEvents).sum(),
                childResults.stream().mapToInt(SkillManagementEventPruneResult::prunedEvents).sum(),
                childResults.stream()
                        .flatMap(result -> result.prunedEventReferences().stream())
                        .toList(),
                childResults.stream().filter(result -> !result.success()).findFirst()
                        .map(SkillManagementEventPruneResult::failure)
                        .orElse(""),
                childResults);
    }

    public boolean success() {
        return failure.isBlank() && children.stream().allMatch(SkillManagementEventPruneResult::success);
    }

    public boolean changed() {
        return success() && !skipped && !dryRun && prunedEvents > 0;
    }
}

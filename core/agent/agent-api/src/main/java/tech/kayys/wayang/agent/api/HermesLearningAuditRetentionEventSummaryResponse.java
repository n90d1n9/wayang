package tech.kayys.wayang.agent.api;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Aggregate REST projection over learning-audit retention journal events.
 */
public record HermesLearningAuditRetentionEventSummaryResponse(
        int totalEvents,
        int attentionEvents,
        int criticalEvents,
        int warningEvents,
        int highestPriority,
        int nearCapacityEvents,
        int atCapacityEvents,
        int maxUtilizationPercent,
        int maxOverflowEntries,
        int minRemainingEntries,
        int latestUtilizationPercent,
        boolean requiresAttention,
        String latestEventId,
        String latestOccurredAt,
        String latestRetentionStatus,
        String latestRetentionSeverity,
        Map<String, Long> retentionStatusCounts,
        Map<String, Long> retentionSeverityCounts,
        Map<String, Long> retentionRecommendedActionCounts,
        List<String> retentionAttention,
        List<HermesOperationalAttention> retentionAttentionItems,
        HermesOperationalAttentionSummaryResponse retentionAttentionSummary,
        List<String> retentionRecommendedActions,
        List<HermesOperationalAction> retentionRecommendedActionItems,
        HermesOperationalActionSummaryResponse retentionRecommendedActionSummary) {

    public HermesLearningAuditRetentionEventSummaryResponse {
        totalEvents = Math.max(totalEvents, 0);
        attentionEvents = Math.max(attentionEvents, 0);
        criticalEvents = Math.max(criticalEvents, 0);
        warningEvents = Math.max(warningEvents, 0);
        highestPriority = Math.max(highestPriority, 0);
        nearCapacityEvents = Math.max(nearCapacityEvents, 0);
        atCapacityEvents = Math.max(atCapacityEvents, 0);
        maxUtilizationPercent = Math.max(maxUtilizationPercent, 0);
        maxOverflowEntries = Math.max(maxOverflowEntries, 0);
        minRemainingEntries = Math.max(minRemainingEntries, 0);
        latestUtilizationPercent = Math.max(latestUtilizationPercent, 0);
        latestEventId = HermesResponseMetadata.text(latestEventId, "");
        latestOccurredAt = HermesResponseMetadata.text(latestOccurredAt, "");
        latestRetentionStatus = HermesResponseMetadata.text(latestRetentionStatus, "");
        latestRetentionSeverity = HermesResponseMetadata.text(latestRetentionSeverity, "");
        retentionStatusCounts = retentionStatusCounts == null ? Map.of() : Map.copyOf(retentionStatusCounts);
        retentionSeverityCounts = retentionSeverityCounts == null ? Map.of() : Map.copyOf(retentionSeverityCounts);
        retentionRecommendedActionCounts = retentionRecommendedActionCounts == null
                ? Map.of()
                : Map.copyOf(retentionRecommendedActionCounts);
        retentionAttention = retentionAttention == null ? List.of() : List.copyOf(retentionAttention);
        retentionAttentionItems = retentionAttentionItems == null
                ? HermesOperationalAttention.fromMessages(
                        "learning-audit-retention",
                        latestRetentionSeverity,
                        highestPriority,
                        retentionAttention)
                : List.copyOf(retentionAttentionItems);
        retentionAttentionSummary = retentionAttentionSummary == null
                ? HermesOperationalAttentionSummaryResponse.from(retentionAttentionItems)
                : retentionAttentionSummary;
        retentionRecommendedActions = retentionRecommendedActions == null
                ? List.of()
                : List.copyOf(retentionRecommendedActions);
        retentionRecommendedActionItems = retentionRecommendedActionItems == null
                ? HermesOperationalAction.retentionActions(
                        latestRetentionSeverity,
                        highestPriority,
                        retentionRecommendedActions)
                : List.copyOf(retentionRecommendedActionItems);
        retentionRecommendedActionSummary = retentionRecommendedActionSummary == null
                ? HermesOperationalActionSummaryResponse.from(retentionRecommendedActionItems)
                : retentionRecommendedActionSummary;
    }

    static HermesLearningAuditRetentionEventSummaryResponse empty() {
        return from(List.of());
    }

    static HermesLearningAuditRetentionEventSummaryResponse from(
            List<HermesLearningAuditRetentionEventResponse> events) {
        List<HermesLearningAuditRetentionEventResponse> values = events == null
                ? List.of()
                : events.stream()
                        .filter(event -> event != null)
                        .toList();
        HermesLearningAuditRetentionEventResponse latest = latest(values);
        int highestPriority = values.stream()
                .mapToInt(HermesLearningAuditRetentionEventResponse::retentionPriority)
                .max()
                .orElse(0);
        String summarySeverity = summarySeverity(values, latest);
        List<String> attention = distinctStrings(values, HermesLearningAuditRetentionEventResponse::retentionAttention);
        List<String> actions = distinctStrings(
                values,
                HermesLearningAuditRetentionEventResponse::retentionRecommendedActions);
        return new HermesLearningAuditRetentionEventSummaryResponse(
                values.size(),
                (int) values.stream()
                        .filter(HermesLearningAuditRetentionEventResponse::retentionRequiresAttention)
                        .count(),
                (int) values.stream()
                        .filter(event -> "critical".equalsIgnoreCase(event.retentionSeverity()))
                        .count(),
                (int) values.stream()
                        .filter(event -> "warning".equalsIgnoreCase(event.retentionSeverity()))
                        .count(),
                highestPriority,
                (int) values.stream()
                        .filter(HermesLearningAuditRetentionEventResponse::nearCapacity)
                        .count(),
                (int) values.stream()
                        .filter(HermesLearningAuditRetentionEventResponse::atCapacity)
                        .count(),
                values.stream()
                        .mapToInt(HermesLearningAuditRetentionEventResponse::utilizationPercent)
                        .max()
                        .orElse(0),
                values.stream()
                        .mapToInt(HermesLearningAuditRetentionEventResponse::overflowEntries)
                        .max()
                        .orElse(0),
                values.stream()
                        .mapToInt(HermesLearningAuditRetentionEventResponse::remainingEntries)
                        .min()
                        .orElse(0),
                latest == null ? 0 : latest.utilizationPercent(),
                values.stream().anyMatch(HermesLearningAuditRetentionEventResponse::retentionRequiresAttention),
                latest == null ? "" : latest.eventId(),
                latest == null ? "" : latest.occurredAt(),
                latest == null ? "" : latest.retentionStatus(),
                summarySeverity,
                counts(values, HermesLearningAuditRetentionEventResponse::retentionStatus),
                counts(values, HermesLearningAuditRetentionEventResponse::retentionSeverity),
                actionCounts(values),
                attention,
                null,
                null,
                actions,
                null,
                null);
    }

    private static HermesLearningAuditRetentionEventResponse latest(
            List<HermesLearningAuditRetentionEventResponse> events) {
        return events.stream()
                .max(Comparator
                        .comparing(
                                (HermesLearningAuditRetentionEventResponse event) -> instant(event.occurredAt()),
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(HermesLearningAuditRetentionEventResponse::eventId))
                .orElse(null);
    }

    private static Instant instant(String value) {
        String text = HermesResponseMetadata.text(value, "");
        if (text.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String summarySeverity(
            List<HermesLearningAuditRetentionEventResponse> events,
            HermesLearningAuditRetentionEventResponse latest) {
        if (events.stream().anyMatch(event -> "critical".equalsIgnoreCase(event.retentionSeverity()))) {
            return "critical";
        }
        if (events.stream().anyMatch(event -> "warning".equalsIgnoreCase(event.retentionSeverity()))) {
            return "warning";
        }
        return latest == null
                ? ""
                : HermesResponseMetadata.text(latest.retentionSeverity(), "");
    }

    private static Map<String, Long> counts(
            List<HermesLearningAuditRetentionEventResponse> events,
            Function<HermesLearningAuditRetentionEventResponse, String> classifier) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (HermesLearningAuditRetentionEventResponse event : events) {
            String value = HermesResponseMetadata.text(classifier.apply(event), "unknown");
            counts.merge(value, 1L, Long::sum);
        }
        return Map.copyOf(counts);
    }

    private static Map<String, Long> actionCounts(List<HermesLearningAuditRetentionEventResponse> events) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (HermesLearningAuditRetentionEventResponse event : events) {
            for (String action : event.retentionRecommendedActions()) {
                String value = HermesResponseMetadata.text(action, "");
                if (!value.isBlank()) {
                    counts.merge(value, 1L, Long::sum);
                }
            }
        }
        return Map.copyOf(counts);
    }

    private static List<String> distinctStrings(
            List<HermesLearningAuditRetentionEventResponse> events,
            Function<HermesLearningAuditRetentionEventResponse, List<String>> classifier) {
        return events.stream()
                .flatMap(event -> classifier.apply(event).stream())
                .map(value -> HermesResponseMetadata.text(value, ""))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }
}

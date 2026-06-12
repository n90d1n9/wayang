package tech.kayys.wayang.agent.api;

import java.util.List;
import java.util.Map;

/**
 * Typed REST payload for Hermes runtime-journal inspections.
 */
public record HermesJournalResponse(
        String port,
        String operation,
        String target,
        boolean active,
        boolean dispatched,
        boolean successful,
        String status,
        String reason,
        Map<String, Object> metadata,
        int matchedEvents,
        int totalMatchedEvents,
        int returnedEvents,
        boolean truncated,
        String previousCursor,
        String nextCursor,
        String firstCursor,
        String lastCursor,
        boolean hasPreviousPage,
        boolean hasNextPage,
        boolean cursorResolved,
        String journalStatus,
        boolean resumable,
        boolean requiresAttention,
        HermesJournalSummaryResponse summary,
        List<HermesJournalEventResponse> events,
        List<HermesLearningAuditRetentionEventResponse> learningAuditRetentionEvents,
        HermesLearningAuditRetentionEventSummaryResponse learningAuditRetentionSummary,
        List<HermesOperationalAttention> operationalAttentionItems,
        HermesOperationalAttentionSummaryResponse operationalAttentionSummary,
        List<HermesOperationalAction> operationalActionItems,
        HermesOperationalActionSummaryResponse operationalActionSummary,
        Map<String, Object> query,
        Map<String, Object> journalView,
        Map<String, Object> sessionSnapshot) {

    public HermesJournalResponse {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        previousCursor = HermesResponseMetadata.text(previousCursor, "");
        nextCursor = HermesResponseMetadata.text(nextCursor, "");
        firstCursor = HermesResponseMetadata.text(firstCursor, "");
        lastCursor = HermesResponseMetadata.text(lastCursor, "");
        journalStatus = HermesResponseMetadata.text(journalStatus, "");
        summary = summary == null ? HermesJournalSummaryResponse.empty() : summary;
        events = events == null
                ? List.of()
                : events.stream()
                        .filter(event -> event != null)
                        .toList();
        learningAuditRetentionEvents = learningAuditRetentionEvents == null
                ? retentionEvents(events)
                : learningAuditRetentionEvents.stream()
                        .filter(event -> event != null)
                        .toList();
        learningAuditRetentionSummary = learningAuditRetentionSummary == null
                ? HermesLearningAuditRetentionEventSummaryResponse.from(learningAuditRetentionEvents)
                : learningAuditRetentionSummary;
        operationalAttentionItems = operationalAttentionItems == null
                ? attentionItems(learningAuditRetentionEvents)
                : operationalAttentionItems.stream()
                        .filter(item -> item != null)
                        .toList();
        operationalAttentionSummary = operationalAttentionSummary == null
                ? HermesOperationalAttentionSummaryResponse.from(operationalAttentionItems)
                : operationalAttentionSummary;
        operationalActionItems = operationalActionItems == null
                ? actionItems(learningAuditRetentionEvents)
                : operationalActionItems.stream()
                        .filter(action -> action != null)
                        .toList();
        operationalActionSummary = operationalActionSummary == null
                ? HermesOperationalActionSummaryResponse.from(operationalActionItems)
                : operationalActionSummary;
        query = query == null ? Map.of() : Map.copyOf(query);
        journalView = journalView == null ? Map.of() : Map.copyOf(journalView);
        sessionSnapshot = sessionSnapshot == null ? Map.of() : Map.copyOf(sessionSnapshot);
    }

    public static HermesJournalResponse from(HermesPortResponse response) {
        HermesPortResponse resolved = response == null ? HermesPortResponse.from(null) : response;
        Map<String, Object> metadata = resolved.metadata();
        return new HermesJournalResponse(
                resolved.port(),
                resolved.operation(),
                resolved.target(),
                resolved.active(),
                resolved.dispatched(),
                resolved.successful(),
                resolved.status(),
                resolved.reason(),
                metadata,
                HermesResponseMetadata.integer(metadata.get("matchedEvents")),
                HermesResponseMetadata.integer(metadata.get("totalMatchedEvents")),
                HermesResponseMetadata.integer(metadata.get("returnedEvents")),
                HermesResponseMetadata.bool(metadata.get("truncated")),
                HermesResponseMetadata.text(metadata.get("previousCursor"), ""),
                HermesResponseMetadata.text(metadata.get("nextCursor"), ""),
                HermesResponseMetadata.text(metadata.get("firstCursor"), ""),
                HermesResponseMetadata.text(metadata.get("lastCursor"), ""),
                HermesResponseMetadata.bool(metadata.get("hasPreviousPage")),
                HermesResponseMetadata.bool(metadata.get("hasNextPage")),
                HermesResponseMetadata.bool(metadata.get("cursorResolved")),
                HermesResponseMetadata.text(metadata.get("status"), ""),
                HermesResponseMetadata.bool(metadata.get("resumable")),
                HermesResponseMetadata.bool(metadata.get("requiresAttention")),
                summary(metadata),
                events(metadata),
                null,
                null,
                null,
                null,
                null,
                null,
                HermesResponseMetadata.objectMap(metadata.get("query")),
                HermesResponseMetadata.objectMap(metadata.get("journalView")),
                HermesResponseMetadata.objectMap(metadata.get("sessionSnapshot")));
    }

    private static HermesJournalSummaryResponse summary(Map<String, Object> metadata) {
        Map<String, Object> direct = HermesResponseMetadata.objectMap(metadata.get("summary"));
        if (!direct.isEmpty()) {
            return HermesJournalSummaryResponse.from(direct);
        }
        Map<String, Object> journalView = HermesResponseMetadata.objectMap(metadata.get("journalView"));
        return HermesJournalSummaryResponse.from(HermesResponseMetadata.objectMap(journalView.get("summary")));
    }

    private static List<HermesJournalEventResponse> events(Map<String, Object> metadata) {
        List<Map<String, Object>> direct = HermesResponseMetadata.objectMaps(metadata.get("events"));
        if (!direct.isEmpty()) {
            return eventResponses(direct);
        }
        Map<String, Object> journalView = HermesResponseMetadata.objectMap(metadata.get("journalView"));
        Map<String, Object> page = HermesResponseMetadata.objectMap(journalView.get("page"));
        return eventResponses(HermesResponseMetadata.objectMaps(page.get("events")));
    }

    private static List<HermesJournalEventResponse> eventResponses(List<Map<String, Object>> events) {
        return events.stream()
                .map(HermesJournalEventResponse::from)
                .toList();
    }

    private static List<HermesLearningAuditRetentionEventResponse> retentionEvents(
            List<HermesJournalEventResponse> events) {
        return events.stream()
                .filter(HermesLearningAuditRetentionEventResponse::isRetentionEvent)
                .map(HermesLearningAuditRetentionEventResponse::from)
                .toList();
    }

    private static List<HermesOperationalAttention> attentionItems(
            List<HermesLearningAuditRetentionEventResponse> retentionEvents) {
        return retentionEvents.stream()
                .flatMap(event -> event.retentionAttentionItems().stream())
                .toList();
    }

    private static List<HermesOperationalAction> actionItems(
            List<HermesLearningAuditRetentionEventResponse> retentionEvents) {
        return retentionEvents.stream()
                .flatMap(event -> event.retentionRecommendedActionItems().stream())
                .toList();
    }
}

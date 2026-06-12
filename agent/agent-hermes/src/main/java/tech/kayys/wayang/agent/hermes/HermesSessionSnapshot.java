package tech.kayys.wayang.agent.hermes;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact resume-oriented state derived from a runtime-journal query window.
 */
public record HermesSessionSnapshot(
        HermesRuntimeEventQuery query,
        String tenantId,
        String sessionId,
        String userId,
        String status,
        boolean resumable,
        boolean terminal,
        boolean requiresAttention,
        int eventCount,
        int matchedEvents,
        int pendingActionCount,
        long failedEvents,
        long successfulEvents,
        long distinctRequests,
        int attentionCount,
        int remediationActionCount,
        String latestEventId,
        String latestEventType,
        String latestRequestId,
        String latestOutcome,
        Instant firstOccurredAt,
        Instant latestOccurredAt,
        List<String> requestIds,
        HermesRuntimeJournalSummary summary) {

    public HermesSessionSnapshot {
        query = query == null ? HermesRuntimeEventQuery.latest() : query;
        tenantId = HermesText.trimToEmpty(tenantId);
        sessionId = HermesText.trimToEmpty(sessionId);
        userId = HermesText.trimToEmpty(userId);
        status = HermesText.trimOr(status, "empty");
        eventCount = Math.max(eventCount, 0);
        matchedEvents = Math.max(matchedEvents, eventCount);
        pendingActionCount = Math.max(pendingActionCount, 0);
        failedEvents = Math.max(failedEvents, 0L);
        successfulEvents = Math.max(successfulEvents, 0L);
        distinctRequests = Math.max(distinctRequests, 0L);
        attentionCount = Math.max(attentionCount, 0);
        remediationActionCount = Math.max(remediationActionCount, 0);
        latestEventId = HermesText.trimToEmpty(latestEventId);
        latestEventType = HermesText.trimToEmpty(latestEventType);
        latestRequestId = HermesText.trimToEmpty(latestRequestId);
        latestOutcome = HermesText.trimToEmpty(latestOutcome);
        requestIds = HermesText.distinctTrimmedList(requestIds);
        summary = summary == null ? HermesRuntimeJournalSummary.empty() : summary;
    }

    public static HermesSessionSnapshot empty(HermesRuntimeEventQuery query) {
        HermesRuntimeEventQuery resolved = query == null ? HermesRuntimeEventQuery.latest() : query;
        return from(HermesRuntimeJournalView.from(
                resolved,
                new HermesRuntimeEventPage(List.of(), 0)));
    }

    public static HermesSessionSnapshot from(HermesRuntimeJournalView view) {
        HermesRuntimeJournalView resolved = view == null
                ? HermesRuntimeJournalView.from(
                        HermesRuntimeEventQuery.latest(),
                        new HermesRuntimeEventPage(List.of(), 0))
                : view;
        HermesRuntimeEventQuery query = resolved.query();
        HermesRuntimeEventPage page = resolved.page();
        List<HermesRuntimeEvent> events = page.events();
        HermesRuntimeJournalSummary summary = resolved.summary();
        HermesRuntimeEvent latest = latest(events);
        int attentionCount = events.stream()
                .mapToInt(HermesSessionSnapshot::attentionCount)
                .sum();
        int remediationActionCount = events.stream()
                .mapToInt(HermesSessionSnapshot::remediationActionCount)
                .sum();
        boolean requiresAttention = requiresAttention(latest, attentionCount, remediationActionCount);
        boolean terminal = terminal(latest);
        String status = status(latest, requiresAttention, summary);
        int pendingActionCount = pendingActionCount(terminal, requiresAttention, attentionCount, remediationActionCount);
        return new HermesSessionSnapshot(
                query,
                identity(query.tenantId(), latest, events, HermesRuntimeEvent::tenantId),
                identity(query.sessionId(), latest, events, HermesRuntimeEvent::sessionId),
                identity(query.userId(), latest, events, HermesRuntimeEvent::userId),
                status,
                !events.isEmpty() && (requiresAttention || !terminal),
                terminal,
                requiresAttention,
                events.size(),
                page.matchedEvents(),
                pendingActionCount,
                summary.failedEvents(),
                summary.successfulEvents(),
                summary.distinctRequests(),
                attentionCount,
                remediationActionCount,
                latest == null ? "" : latest.eventId(),
                latest == null ? "" : latest.type(),
                latest == null ? "" : latest.requestId(),
                latest == null ? "" : latest.outcome(),
                summary.firstOccurredAt(),
                summary.latestOccurredAt(),
                requestIds(events),
                summary);
    }

    public boolean hasEvents() {
        return eventCount > 0;
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("query", query.toMetadata());
        metadata.put("tenantId", tenantId);
        metadata.put("sessionId", sessionId);
        metadata.put("userId", userId);
        metadata.put("status", status);
        metadata.put("resumable", resumable);
        metadata.put("terminal", terminal);
        metadata.put("requiresAttention", requiresAttention);
        metadata.put("eventCount", eventCount);
        metadata.put("matchedEvents", matchedEvents);
        metadata.put("pendingActionCount", pendingActionCount);
        metadata.put("failedEvents", failedEvents);
        metadata.put("successfulEvents", successfulEvents);
        metadata.put("distinctRequests", distinctRequests);
        metadata.put("attentionCount", attentionCount);
        metadata.put("remediationActionCount", remediationActionCount);
        metadata.put("latestEventId", latestEventId);
        metadata.put("latestEventType", latestEventType);
        metadata.put("latestRequestId", latestRequestId);
        metadata.put("latestOutcome", latestOutcome);
        metadata.put("firstOccurredAt", firstOccurredAt == null ? "" : firstOccurredAt.toString());
        metadata.put("latestOccurredAt", latestOccurredAt == null ? "" : latestOccurredAt.toString());
        metadata.put("requestIds", requestIds);
        metadata.put("summary", summary.toMetadata());
        return Map.copyOf(metadata);
    }

    private static HermesRuntimeEvent latest(List<HermesRuntimeEvent> events) {
        return events.stream()
                .max(Comparator.comparing(HermesRuntimeEvent::occurredAt)
                        .thenComparing(HermesRuntimeEvent::eventId))
                .orElse(null);
    }

    private static boolean terminal(HermesRuntimeEvent latest) {
        return latest != null
                && (HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED.equals(latest.type())
                || HermesRuntimeEvent.TYPE_RESPONSE_FAILED.equals(latest.type())
                || HermesRuntimeEvent.TYPE_SKILL_LEARNING_COMPLETED.equals(latest.type())
                || HermesRuntimeEvent.TYPE_SKILL_LEARNING_FAILED.equals(latest.type()));
    }

    private static boolean requiresAttention(
            HermesRuntimeEvent latest,
            int attentionCount,
            int remediationActionCount) {
        if (attentionCount > 0 || remediationActionCount > 0) {
            return true;
        }
        if (latest == null) {
            return false;
        }
        String outcome = HermesText.trimToEmpty(latest.outcome());
        return "failed".equalsIgnoreCase(outcome)
                || "degraded".equalsIgnoreCase(outcome)
                || "blocked".equalsIgnoreCase(outcome);
    }

    private static String status(
            HermesRuntimeEvent latest,
            boolean requiresAttention,
            HermesRuntimeJournalSummary summary) {
        if (latest == null || !summary.hasEvents()) {
            return "empty";
        }
        if (requiresAttention) {
            return "needs-attention";
        }
        if (HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED.equals(latest.type())) {
            return "completed";
        }
        if (HermesRuntimeEvent.TYPE_RESPONSE_FAILED.equals(latest.type())) {
            return "failed";
        }
        if (HermesRuntimeEvent.TYPE_SKILL_LEARNING_COMPLETED.equals(latest.type())) {
            return "completed";
        }
        if (HermesRuntimeEvent.TYPE_SKILL_LEARNING_FAILED.equals(latest.type())) {
            return "failed";
        }
        if (HermesRuntimeEvent.TYPE_DIRECTIVES_DISPATCHED.equals(latest.type())) {
            return "dispatched";
        }
        if (HermesRuntimeEvent.TYPE_REQUEST_PLANNED.equals(latest.type())) {
            return "planned";
        }
        return "active";
    }

    private static int pendingActionCount(
            boolean terminal,
            boolean requiresAttention,
            int attentionCount,
            int remediationActionCount) {
        if (requiresAttention) {
            return Math.max(1, Math.max(attentionCount, remediationActionCount));
        }
        return terminal ? 0 : 1;
    }

    private static int attentionCount(HermesRuntimeEvent event) {
        if (event == null) {
            return 0;
        }
        int direct = intValue(event.metadata().get("attentionCount"));
        if (direct > 0) {
            return direct;
        }
        return intValue(mapValue(event.metadata().get("dispatchReport"), "attentionCount"));
    }

    private static int remediationActionCount(HermesRuntimeEvent event) {
        if (event == null) {
            return 0;
        }
        Object plan = event.metadata().get("remediationPlan");
        int direct = intValue(mapValue(plan, "actionCount"));
        if (direct > 0) {
            return direct;
        }
        Object reportPlan = mapValue(event.metadata().get("dispatchReport"), "remediationPlan");
        return intValue(mapValue(reportPlan, "actionCount"));
    }

    private static List<String> requestIds(List<HermesRuntimeEvent> events) {
        return events.stream()
                .map(HermesRuntimeEvent::requestId)
                .map(HermesText::trimToEmpty)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static String identity(
            String requested,
            HermesRuntimeEvent latest,
            List<HermesRuntimeEvent> events,
            java.util.function.Function<HermesRuntimeEvent, String> classifier) {
        String requestedValue = HermesText.trimToEmpty(requested);
        if (!requestedValue.isBlank()) {
            return requestedValue;
        }
        if (latest != null) {
            String latestValue = HermesText.trimToEmpty(classifier.apply(latest));
            if (!latestValue.isBlank()) {
                return latestValue;
            }
        }
        return events.stream()
                .map(classifier)
                .map(HermesText::trimToEmpty)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private static Object mapValue(Object value, String key) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        return map.get(key);
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return Math.max(number.intValue(), 0);
        }
        if (value == null) {
            return 0;
        }
        try {
            return Math.max(Integer.parseInt(String.valueOf(value)), 0);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

}

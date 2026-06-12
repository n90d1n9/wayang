package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JSONL codec shared by transfer audit stores.
 */
final class AgenticCommerceWayangPersistenceTransferAuditJsonl {

    static final String JOURNAL_FORMAT =
            "agentic-commerce-wayang-persistence-transfer-audit-v1";
    static final String MIME_JSONL = "application/x-ndjson";

    private AgenticCommerceWayangPersistenceTransferAuditJsonl() {
    }

    static String toJsonLine(AgenticCommerceWayangPersistenceTransferAuditTrail trail) {
        return AgenticCommerceJson.write(toJournalMap(trail));
    }

    static Optional<AgenticCommerceWayangPersistenceTransferAuditTrail> fromJsonLine(String line) {
        String resolvedLine = AgenticCommerceWayangMaps.text(line);
        if (resolvedLine.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(AgenticCommerceWayangPersistenceTransferAuditTrail.fromMap(
                    AgenticCommerceJson.readObject(resolvedLine)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    static List<String> linesFromBody(String body) {
        String resolvedBody = body == null ? "" : body;
        if (resolvedBody.isBlank()) {
            return List.of();
        }
        return resolvedBody.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    static String bodyFromLines(List<String> lines) {
        List<String> retained = retainedLines(lines, Integer.MAX_VALUE);
        if (retained.isEmpty()) {
            return "";
        }
        return String.join(System.lineSeparator(), retained) + System.lineSeparator();
    }

    static List<String> retainedLines(List<String> lines, int maxTrails) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<String> normalized = lines.stream()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        int capacity = maxTrails < 1 ? Integer.MAX_VALUE : maxTrails;
        if (normalized.size() <= capacity) {
            return normalized;
        }
        return normalized.subList(normalized.size() - capacity, normalized.size());
    }

    static List<String> retainedLines(
            List<String> lines,
            AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy) {
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy resolved = retentionPolicy == null
                ? AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.defaults()
                : retentionPolicy;
        return resolved.retainLines(lines);
    }

    private static Map<String, Object> toJournalMap(
            AgenticCommerceWayangPersistenceTransferAuditTrail trail) {
        AgenticCommerceWayangPersistenceTransferAuditSummary summary = trail.summary();
        AgenticCommerceWayangPersistenceTransferAuditDecision decision = summary.decision();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("journalFormat", JOURNAL_FORMAT);
        values.put("trailType", trail.trailType());
        values.put("trailStatus", trail.trailStatus());
        values.put("outcomeStatus", summary.outcomeStatus());
        values.put("nextAction", decision.nextAction());
        values.put("decisionStatus", decision.decisionStatus());
        values.put("requiresAttention", summary.requiresAttention());
        values.put("eventCount", trail.eventCount());
        values.put("trail", trail.toMap());
        return values;
    }
}

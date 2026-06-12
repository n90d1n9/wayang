package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded query result for persistence transfer audit trails.
 */
public record AgenticCommerceWayangPersistenceTransferAuditPage(
        AgenticCommerceWayangPersistenceTransferAuditQuery query,
        List<AgenticCommerceWayangPersistenceTransferAuditTrail> trails,
        int totalTrailCount,
        int returnedTrailCount,
        boolean truncated,
        Map<String, Object> attributes) {

    public AgenticCommerceWayangPersistenceTransferAuditPage {
        query = query == null
                ? AgenticCommerceWayangPersistenceTransferAuditQuery.latest(
                        AgenticCommerceWayangPersistenceTransferAuditQuery.DEFAULT_LIMIT)
                : query;
        trails = normalizeTrails(trails);
        totalTrailCount = Math.max(totalTrailCount, trails.size());
        returnedTrailCount = trails.size();
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferAuditPage from(
            List<AgenticCommerceWayangPersistenceTransferAuditTrail> trails,
            AgenticCommerceWayangPersistenceTransferAuditQuery query) {
        AgenticCommerceWayangPersistenceTransferAuditQuery resolvedQuery = query == null
                ? AgenticCommerceWayangPersistenceTransferAuditQuery.latest(
                        AgenticCommerceWayangPersistenceTransferAuditQuery.DEFAULT_LIMIT)
                : query;
        List<AgenticCommerceWayangPersistenceTransferAuditTrail> matched = normalizeTrails(trails).stream()
                .filter(resolvedQuery::matches)
                .toList();
        int returnedFrom = Math.max(0, matched.size() - resolvedQuery.limit());
        List<AgenticCommerceWayangPersistenceTransferAuditTrail> returned = matched.subList(returnedFrom, matched.size());
        return new AgenticCommerceWayangPersistenceTransferAuditPage(
                resolvedQuery,
                returned,
                matched.size(),
                returned.size(),
                matched.size() > returned.size(),
                attributes(trails, matched));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("totalTrailCount", totalTrailCount);
        values.put("returnedTrailCount", returnedTrailCount);
        values.put("truncated", truncated);
        values.put("query", query.toMap());
        values.put("trailTypes", trailTypes());
        values.put("outcomeStatuses", outcomeStatuses());
        values.put("nextActions", nextActions());
        values.put("trails", trails.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditTrail::toMap)
                .toList());
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    public List<String> trailTypes() {
        return trails.stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditTrail::trailType)
                .distinct()
                .toList();
    }

    public List<String> outcomeStatuses() {
        return trails.stream()
                .map(trail -> trail.summary().outcomeStatus())
                .distinct()
                .toList();
    }

    public List<String> nextActions() {
        return trails.stream()
                .map(trail -> trail.summary().decision().nextAction())
                .distinct()
                .toList();
    }

    private static List<AgenticCommerceWayangPersistenceTransferAuditTrail> normalizeTrails(
            List<AgenticCommerceWayangPersistenceTransferAuditTrail> trails) {
        if (trails == null || trails.isEmpty()) {
            return List.of();
        }
        return trails.stream()
                .filter(trail -> trail != null && !trail.trailType().isBlank())
                .toList();
    }

    private static Map<String, Object> attributes(
            List<AgenticCommerceWayangPersistenceTransferAuditTrail> source,
            List<AgenticCommerceWayangPersistenceTransferAuditTrail> matched) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceTrailCount", normalizeTrails(source).size());
        values.put("matchedTrailCount", matched.size());
        return Map.copyOf(values);
    }
}

package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of running the transfer audit sink/reader contract harness.
 */
public record AgenticCommerceWayangPersistenceTransferAuditContractReport(
        String contractId,
        int expectedRetainedTrailCount,
        int retainedTrailCount,
        int reloadTrailCount,
        boolean reloadAttempted,
        List<String> issues,
        Map<String, Object> retainedPage,
        Map<String, Object> reloadPage,
        Map<String, Object> attributes) {

    public AgenticCommerceWayangPersistenceTransferAuditContractReport {
        contractId = AgenticCommerceWayangMaps.required(contractId, "contractId");
        expectedRetainedTrailCount = Math.max(1, expectedRetainedTrailCount);
        retainedTrailCount = Math.max(0, retainedTrailCount);
        reloadTrailCount = Math.max(0, reloadTrailCount);
        issues = AgenticCommerceWayangMaps.stringList(issues);
        retainedPage = AgenticCommerceWayangMaps.copy(retainedPage);
        reloadPage = AgenticCommerceWayangMaps.copy(reloadPage);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public boolean passed() {
        return issues.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }

    public AgenticCommerceWayangPersistenceTransferAuditContractReport withAttributes(
            Map<String, Object> extraAttributes) {
        Map<String, Object> merged = new LinkedHashMap<>(attributes);
        merged.putAll(AgenticCommerceWayangMaps.copy(extraAttributes));
        return new AgenticCommerceWayangPersistenceTransferAuditContractReport(
                contractId,
                expectedRetainedTrailCount,
                retainedTrailCount,
                reloadTrailCount,
                reloadAttempted,
                issues,
                retainedPage,
                reloadPage,
                merged);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contractId", contractId);
        values.put("passed", passed());
        values.put("issueCount", issueCount());
        values.put("issues", issues);
        values.put("expectedRetainedTrailCount", expectedRetainedTrailCount);
        values.put("retainedTrailCount", retainedTrailCount);
        values.put("reloadTrailCount", reloadTrailCount);
        values.put("reloadAttempted", reloadAttempted);
        values.put("retainedPage", retainedPage);
        values.put("reloadPage", reloadPage);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }
}

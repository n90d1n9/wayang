package tech.kayys.wayang.agenticcommerce.wayang;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Retained-history capacity assessment for a transfer audit retention policy.
 */
public record AgenticCommerceWayangPersistenceTransferAuditRetentionPolicyAssessment(
        int expectedRetainedTrailCount,
        int retainedTrailCount,
        int sampleTrailCount,
        long maxBytes,
        long recommendedMinBytes,
        boolean byteLimited,
        boolean satisfiesContract) {

    public AgenticCommerceWayangPersistenceTransferAuditRetentionPolicyAssessment {
        expectedRetainedTrailCount = Math.max(1, expectedRetainedTrailCount);
        retainedTrailCount = Math.max(0, retainedTrailCount);
        sampleTrailCount = Math.max(0, sampleTrailCount);
        maxBytes = Math.max(AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.UNLIMITED_BYTES, maxBytes);
        recommendedMinBytes = Math.max(0L, recommendedMinBytes);
        satisfiesContract = retainedTrailCount >= expectedRetainedTrailCount;
    }

    public static AgenticCommerceWayangPersistenceTransferAuditRetentionPolicyAssessment from(
            AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy,
            List<String> sampleLines,
            int expectedRetainedTrailCount) {
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy resolved = retentionPolicy == null
                ? AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.defaults()
                : retentionPolicy;
        List<String> samples = sampleLines == null ? List.of() : sampleLines;
        int expected = Math.min(samples.size(), Math.max(1, expectedRetainedTrailCount));
        List<String> retained = resolved.retainLines(samples);
        return new AgenticCommerceWayangPersistenceTransferAuditRetentionPolicyAssessment(
                expected,
                retained.size(),
                samples.size(),
                resolved.maxBytes(),
                recommendedMinBytes(samples, expected),
                resolved.byteLimited(),
                retained.size() >= expected);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("expectedRetainedTrailCount", expectedRetainedTrailCount);
        values.put("retainedTrailCount", retainedTrailCount);
        values.put("sampleTrailCount", sampleTrailCount);
        values.put("maxBytes", maxBytes);
        values.put("maxBytesDisplay", AgenticCommerceWayangByteSizes.formatLimit(maxBytes));
        values.put("recommendedMinBytes", recommendedMinBytes);
        values.put("recommendedMinBytesDisplay", AgenticCommerceWayangByteSizes.format(recommendedMinBytes));
        values.put("byteLimited", byteLimited);
        values.put("satisfiesContract", satisfiesContract);
        return Map.copyOf(values);
    }

    private static long recommendedMinBytes(List<String> lines, int expectedRetainedTrailCount) {
        if (lines == null || lines.isEmpty() || expectedRetainedTrailCount < 1) {
            return 0L;
        }
        int fromIndex = Math.max(0, lines.size() - expectedRetainedTrailCount);
        return lines.subList(fromIndex, lines.size()).stream()
                .mapToLong(AgenticCommerceWayangPersistenceTransferAuditRetentionPolicyAssessment::lineBytes)
                .sum();
    }

    private static long lineBytes(String line) {
        String normalized = AgenticCommerceWayangMaps.text(line);
        if (normalized.isBlank()) {
            return 0L;
        }
        return normalized.getBytes(StandardCharsets.UTF_8).length
                + System.lineSeparator().getBytes(StandardCharsets.UTF_8).length;
    }
}

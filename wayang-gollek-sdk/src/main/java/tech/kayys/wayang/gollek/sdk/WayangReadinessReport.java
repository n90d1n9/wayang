package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared readiness envelope for protocol-specific Wayang adapter probes.
 */
public record WayangReadinessReport(
        String readinessId,
        boolean ready,
        int exitCode,
        int issueCount,
        List<Map<String, Object>> probes,
        List<Map<String, Object>> issues,
        Map<String, Object> attributes) {

    public static final String DEFAULT_READINESS_ID = "wayang.readiness";

    public WayangReadinessReport {
        readinessId = SdkText.trimToDefault(readinessId, DEFAULT_READINESS_ID);
        exitCode = Math.max(0, exitCode);
        probes = WayangReportMaps.copyObjects(probes);
        issues = WayangReportMaps.copyObjects(issues);
        issueCount = Math.max(Math.max(0, issueCount), issues.size());
        attributes = WayangReportMaps.copyMap(attributes);
    }

    public static WayangReadinessReport from(
            String readinessId,
            boolean ready,
            int exitCode,
            int issueCount,
            List<Map<String, Object>> probes,
            List<Map<String, Object>> issues,
            Map<String, Object> attributes) {
        return new WayangReadinessReport(
                readinessId,
                ready,
                exitCode,
                issueCount,
                probes,
                issues,
                attributes);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("readinessId", readinessId);
        values.put("ready", ready);
        values.put("exitCode", exitCode);
        values.put("issueCount", issueCount);
        values.put("probes", probes);
        values.put("issues", issues);
        values.put("attributes", attributes);
        return WayangReportMaps.copyMap(values);
    }
}

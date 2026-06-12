package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared diagnostics envelope for protocol-specific Wayang adapters.
 */
public record WayangDiagnosticsReport(
        String diagnosticsId,
        WayangDiagnosticsStatus status,
        int issueCount,
        List<Map<String, Object>> checks,
        List<Map<String, Object>> issues,
        Map<String, Object> attributes) {

    public static final String DEFAULT_DIAGNOSTICS_ID = "wayang.diagnostics";

    public WayangDiagnosticsReport {
        diagnosticsId = SdkText.trimToDefault(diagnosticsId, DEFAULT_DIAGNOSTICS_ID);
        status = status == null ? WayangDiagnosticsStatus.from(false, 1) : status;
        checks = WayangReportMaps.copyObjects(checks);
        issues = WayangReportMaps.copyObjects(issues);
        issueCount = Math.max(Math.max(0, issueCount), issues.size());
        attributes = WayangReportMaps.copyMap(attributes);
    }

    public static WayangDiagnosticsReport from(
            String diagnosticsId,
            boolean passed,
            int exitCode,
            int issueCount,
            List<Map<String, Object>> checks,
            List<Map<String, Object>> issues,
            Map<String, Object> attributes) {
        return new WayangDiagnosticsReport(
                diagnosticsId,
                WayangDiagnosticsStatus.from(passed, exitCode),
                issueCount,
                checks,
                issues,
                attributes);
    }

    public boolean passed() {
        return status.passed();
    }

    public int exitCode() {
        return status.exitCode();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("diagnosticsId", diagnosticsId);
        values.putAll(status.toMap());
        values.put("issueCount", issueCount);
        values.put("checks", checks);
        values.put("issues", issues);
        values.put("attributes", attributes);
        return WayangReportMaps.copyMap(values);
    }
}

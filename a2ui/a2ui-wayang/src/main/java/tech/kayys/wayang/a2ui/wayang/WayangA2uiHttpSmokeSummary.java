package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;

/**
 * Compact consumer-facing summary decoded from an A2UI HTTP smoke response.
 */
public record WayangA2uiHttpSmokeSummary(
        boolean passed,
        int exitCode,
        String suiteId,
        int scenarioCount,
        int issueCount,
        int routeCount,
        boolean smokeResult,
        List<Map<String, Object>> issues,
        Map<String, Object> metadata,
        Map<String, Object> body) {

    public WayangA2uiHttpSmokeSummary {
        exitCode = Math.max(0, exitCode);
        suiteId = suiteId == null ? "" : suiteId.trim();
        scenarioCount = Math.max(0, scenarioCount);
        issueCount = Math.max(0, issueCount);
        routeCount = Math.max(0, routeCount);
        issues = WayangA2uiTransportMaps.copyMaps(issues);
        issueCount = Math.max(issueCount, issues.size());
        metadata = WayangA2uiTransportMaps.copy(metadata);
        body = WayangA2uiTransportMaps.copy(body);
    }

    public static WayangA2uiHttpSmokeSummary from(WayangA2uiHttpResponse response) {
        return WayangA2uiHttpSmokeSummaryDecoder.from(response);
    }

    public static WayangA2uiHttpSmokeSummary from(WayangA2uiTransportResponse response) {
        return WayangA2uiHttpSmokeSummaryDecoder.from(response);
    }

    public static WayangA2uiHttpSmokeSummary fromResultJson(String resultJson) {
        return WayangA2uiHttpSmokeSummaryDecoder.fromResultJson(resultJson);
    }

    public static WayangA2uiHttpSmokeSummary fromMap(Map<?, ?> values) {
        return WayangA2uiHttpSmokeSummaryDecoder.fromMap(values);
    }

    public static WayangA2uiHttpSmokeSummary fromJson(String json) {
        return WayangA2uiHttpSmokeSummaryDecoder.fromJson(json);
    }

    public boolean successfulExit() {
        return passed && exitCode == WayangA2uiHttpSmokeResult.EXIT_SUCCESS;
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpSmokeProbeProjection.summary(this);
    }

    public String toJson() {
        return WayangA2uiTransportJson.json(toMap(), "Unable to encode A2UI HTTP smoke summary");
    }

}

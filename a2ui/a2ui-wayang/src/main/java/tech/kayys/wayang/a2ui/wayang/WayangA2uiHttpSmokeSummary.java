package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpSmokeProbeProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;

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
        exitCode = RecordNumbers.nonNegative(exitCode);
        suiteId = RecordValues.text(suiteId);
        scenarioCount = RecordNumbers.nonNegative(scenarioCount);
        issueCount = RecordNumbers.nonNegative(issueCount);
        routeCount = RecordNumbers.nonNegative(routeCount);
        issues = TransportMaps.copyMaps(issues);
        issueCount = Math.max(issueCount, issues.size());
        metadata = TransportMaps.copy(metadata);
        body = TransportMaps.copy(body);
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

    public static WayangA2uiHttpSmokeSummary empty() {
        return new WayangA2uiHttpSmokeSummary(
                false,
                WayangA2uiHttpSmokeResult.EXIT_FAILURE,
                "",
                0,
                0,
                0,
                false,
                List.of(),
                Map.of(),
                Map.of());
    }

    public boolean successfulExit() {
        return passed && exitCode == WayangA2uiHttpSmokeResult.EXIT_SUCCESS;
    }

    public Map<String, Object> toMap() {
        return HttpSmokeProbeProjection.summary(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP smoke summary");
    }

}

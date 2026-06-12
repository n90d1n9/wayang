package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpSmokeProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpSmokeSummary;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2UI HTTP smoke probe results.
 */
public final class HttpSmokeProbeProjection {

    private HttpSmokeProbeProjection() {
    }

    public static Map<String, Object> probe(WayangA2uiHttpSmokeProbeResult probe) {
        WayangA2uiHttpSmokeProbeResult resolved = Objects.requireNonNull(probe, "probe");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", resolved.statusCode());
        values.put("httpSuccessful", resolved.httpSuccessful());
        values.put("routeOperation", resolved.routeOperation());
        values.put("allow", resolved.allow());
        values.put(WayangA2uiTransportFields.OUTCOME, resolved.outcome());
        values.put("smokeRoute", resolved.smokeRoute());
        values.put(WayangA2uiTransportFields.PASSED, resolved.passed());
        values.put(WayangA2uiTransportFields.EXIT_CODE, resolved.exitCode());
        values.put("summary", resolved.summary().toMap());
        values.put("headers", resolved.headers());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> summary(WayangA2uiHttpSmokeSummary summary) {
        WayangA2uiHttpSmokeSummary resolved = Objects.requireNonNull(summary, "summary");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiTransportFields.PASSED, resolved.passed());
        values.put(WayangA2uiTransportFields.EXIT_CODE, resolved.exitCode());
        values.put(WayangA2uiTransportFields.SUITE_ID, resolved.suiteId());
        values.put(WayangA2uiTransportFields.SCENARIO_COUNT, resolved.scenarioCount());
        values.put(WayangA2uiTransportFields.ISSUE_COUNT, resolved.issueCount());
        values.put(WayangA2uiTransportFields.ROUTE_COUNT, resolved.routeCount());
        values.put("smokeResult", resolved.smokeResult());
        values.put("successfulExit", resolved.successfulExit());
        values.put("issues", resolved.issues());
        values.put(WayangA2uiTransportFields.METADATA, resolved.metadata());
        values.put(WayangA2uiTransportFields.BODY, resolved.body());
        return TransportMaps.freeze(values);
    }

    public static List<Map<String, Object>> issues(
            Map<String, Object> suiteReport,
            Map<String, Object> expectationResult) {
        List<Map<String, Object>> values = new ArrayList<>();
        appendIssues(values, "suite", field(suiteReport, "issues"));
        appendIssues(values, "expectation", field(expectationResult, "validationIssues"));
        return List.copyOf(values);
    }

    private static Object field(Map<String, Object> values, String key) {
        return values == null ? null : values.get(key);
    }

    private static void appendIssues(List<Map<String, Object>> values, String source, Object issues) {
        if (!(issues instanceof List<?> list)) {
            return;
        }
        for (Object issue : list) {
            if (issue instanceof Map<?, ?> map) {
                values.add(HttpIssueMaps.copiedIssueWithSource(map, source));
            }
        }
    }
}

package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bool;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.child;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.number;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.text;

/**
 * Parser and ordered projection for A2A JSON-RPC smoke summaries.
 */
final class WayangA2aJsonRpcSmokeSummaryProjection {

    private WayangA2aJsonRpcSmokeSummaryProjection() {
    }

    static WayangA2aJsonRpcSmokeSummary fromMap(Map<?, ?> bodyValues) {
        Map<String, Object> body = WayangA2aMaps.copyMap(bodyValues);
        Map<String, Object> scenarioResult = child(body, "scenarioResult");
        Map<String, Object> attributes = child(body, "attributes");
        boolean compactSummary = scenarioResult.isEmpty()
                && (body.containsKey("smokeResult") || body.containsKey("successfulExit"));
        List<Map<String, Object>> issues = issues(scenarioResult);
        if (issues.isEmpty()) {
            issues = WayangA2aMaps.objectList(body.get("issues"));
        }
        boolean passed = bool(body.get("passed"), bool(scenarioResult.get("passed"), false));
        return new WayangA2aJsonRpcSmokeSummary(
                passed,
                number(body.get("exitCode"),
                        passed
                                ? WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS
                                : WayangA2aJsonRpcSmokeResult.EXIT_FAILURE),
                text(body.get("scenarioId"),
                        text(attributes.get("scenarioId"), text(scenarioResult.get("scenarioId"), ""))),
                number(body.get("exchangeCount"),
                        number(attributes.get("exchangeCount"), number(scenarioResult.get("exchangeCount"), 0))),
                Math.max(number(body.get("issueCount"), number(scenarioResult.get("issueCount"), 0)), issues.size()),
                bool(body.get("smokeResult"), !scenarioResult.isEmpty()),
                issues,
                attributes,
                compactSummary ? child(body, "body") : body);
    }

    static Map<String, Object> summary(
            boolean passed,
            int exitCode,
            String scenarioId,
            int exchangeCount,
            int issueCount,
            boolean smokeResult,
            boolean successfulExit,
            List<Map<String, Object>> issues,
            Map<String, Object> attributes,
            Map<String, Object> body) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", passed);
        values.put("exitCode", exitCode);
        values.put("scenarioId", scenarioId);
        values.put("exchangeCount", exchangeCount);
        values.put("issueCount", issueCount);
        values.put("smokeResult", smokeResult);
        values.put("successfulExit", successfulExit);
        values.put("issues", issues);
        values.put("attributes", WayangA2aMaps.copyMap(attributes));
        values.put("body", WayangA2aMaps.copyMap(body));
        return WayangA2aMaps.copyMap(values);
    }

    private static List<Map<String, Object>> issues(Map<String, Object> scenarioResult) {
        List<Map<String, Object>> values = new ArrayList<>();
        appendIssues(values, scenarioResult.get("issues"));
        return List.copyOf(values);
    }

    private static void appendIssues(List<Map<String, Object>> values, Object issues) {
        if (!(issues instanceof List<?> list)) {
            return;
        }
        for (Object issue : list) {
            if (issue instanceof Map<?, ?> map) {
                Map<String, Object> copy = new LinkedHashMap<>(WayangA2aMaps.copyMap(map));
                copy.putIfAbsent("source", "scenario");
                values.add(WayangA2aMaps.copyMap(copy));
            }
        }
    }
}

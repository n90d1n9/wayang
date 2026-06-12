package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bodyMap;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyMaps;

/**
 * Compact consumer-facing summary decoded from an A2A JSON-RPC smoke result.
 */
public record WayangA2aJsonRpcSmokeSummary(
        boolean passed,
        int exitCode,
        String scenarioId,
        int exchangeCount,
        int issueCount,
        boolean smokeResult,
        List<Map<String, Object>> issues,
        Map<String, Object> attributes,
        Map<String, Object> body) {

    public WayangA2aJsonRpcSmokeSummary {
        exitCode = Math.max(0, exitCode);
        scenarioId = scenarioId == null ? "" : scenarioId.trim();
        exchangeCount = Math.max(0, exchangeCount);
        issueCount = Math.max(0, issueCount);
        issues = copyMaps(issues);
        issueCount = Math.max(issueCount, issues.size());
        attributes = WayangA2aMaps.copyMap(attributes);
        body = WayangA2aMaps.copyMap(body);
    }

    public static WayangA2aJsonRpcSmokeSummary from(WayangA2aJsonRpcSmokeResult result) {
        return fromMap(Objects.requireNonNull(result, "result").toMap());
    }

    public static WayangA2aJsonRpcSmokeSummary fromResultJson(String resultJson) {
        return fromMap(bodyMap(resultJson));
    }

    public static WayangA2aJsonRpcSmokeSummary fromMap(Map<?, ?> bodyValues) {
        return WayangA2aJsonRpcSmokeSummaryProjection.fromMap(bodyValues);
    }

    public boolean successfulExit() {
        return passed && exitCode == WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS;
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcSmokeSummaryProjection.summary(
                passed,
                exitCode,
                scenarioId,
                exchangeCount,
                issueCount,
                smokeResult,
                successfulExit(),
                issues,
                attributes,
                body);
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }

}

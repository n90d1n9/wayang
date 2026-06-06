package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Stable machine-readable projection of an A2UI HTTP scenario suite result.
 */
public record WayangA2uiHttpScenarioSuiteReport(
        String suiteId,
        int scenarioCount,
        long passedScenarioCount,
        long failedScenarioCount,
        long exchangeCount,
        long successfulCount,
        long clientErrorCount,
        long serverErrorCount,
        long handledCount,
        long rejectedCount,
        boolean transportErrors,
        long issueCount,
        List<String> scenarioIds,
        List<Map<String, Object>> scenarios,
        List<Map<String, Object>> issues,
        Map<String, Object> attributes) {

    public WayangA2uiHttpScenarioSuiteReport {
        suiteId = suiteId == null || suiteId.isBlank() ? "a2ui-http-suite" : suiteId.trim();
        scenarioCount = Math.max(0, scenarioCount);
        passedScenarioCount = Math.max(0L, passedScenarioCount);
        failedScenarioCount = Math.max(0L, failedScenarioCount);
        exchangeCount = Math.max(0L, exchangeCount);
        successfulCount = Math.max(0L, successfulCount);
        clientErrorCount = Math.max(0L, clientErrorCount);
        serverErrorCount = Math.max(0L, serverErrorCount);
        handledCount = Math.max(0L, handledCount);
        rejectedCount = Math.max(0L, rejectedCount);
        issueCount = Math.max(0L, issueCount);
        scenarioIds = scenarioIds == null
                ? List.of()
                : scenarioIds.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .toList();
        scenarios = WayangA2uiTransportMaps.copyMaps(scenarios);
        issues = WayangA2uiTransportMaps.copyMaps(issues);
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpScenarioSuiteReport from(WayangA2uiHttpScenarioSuiteResult result) {
        WayangA2uiHttpScenarioSuiteResult resolved = Objects.requireNonNull(result, "result");
        List<WayangA2uiHttpScenarioReport> reports = resolved.reports();
        return new WayangA2uiHttpScenarioSuiteReport(
                resolved.suiteId(),
                resolved.scenarioCount(),
                resolved.passedScenarioCount(),
                resolved.failedScenarioCount(),
                resolved.exchangeCount(),
                resolved.successfulCount(),
                resolved.clientErrorCount(),
                resolved.serverErrorCount(),
                resolved.handledCount(),
                resolved.rejectedCount(),
                resolved.hasTransportErrors(),
                reports.stream()
                        .mapToLong(WayangA2uiHttpScenarioReport::issueCount)
                        .sum(),
                resolved.scenarioIds(),
                reports.stream()
                        .map(WayangA2uiHttpScenarioReport::toMap)
                        .toList(),
                reports.stream()
                        .flatMap(report -> report.issues().stream())
                        .toList(),
                resolved.attributes());
    }

    public boolean passed() {
        return failedScenarioCount == 0L
                && clientErrorCount == 0L
                && serverErrorCount == 0L
                && issueCount == 0L
                && !transportErrors;
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpScenarioProjection.suiteReport(this);
    }

    public String toJson() {
        return WayangA2uiTransportJson.json(toMap(), "Unable to encode A2UI HTTP scenario suite report");
    }
}

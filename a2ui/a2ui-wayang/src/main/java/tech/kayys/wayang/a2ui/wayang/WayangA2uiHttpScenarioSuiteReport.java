package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpScenarioProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpScenarioSuiteMetrics;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

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
        suiteId = RecordValues.textOrDefault(suiteId, "a2ui-http-suite");
        scenarioCount = RecordNumbers.nonNegative(scenarioCount);
        passedScenarioCount = RecordNumbers.nonNegative(passedScenarioCount);
        failedScenarioCount = RecordNumbers.nonNegative(failedScenarioCount);
        exchangeCount = RecordNumbers.nonNegative(exchangeCount);
        successfulCount = RecordNumbers.nonNegative(successfulCount);
        clientErrorCount = RecordNumbers.nonNegative(clientErrorCount);
        serverErrorCount = RecordNumbers.nonNegative(serverErrorCount);
        handledCount = RecordNumbers.nonNegative(handledCount);
        rejectedCount = RecordNumbers.nonNegative(rejectedCount);
        issueCount = RecordNumbers.nonNegative(issueCount);
        scenarioIds = DecodeCollections.nonBlankTexts(scenarioIds);
        scenarios = TransportMaps.copyMaps(scenarios);
        issues = TransportMaps.copyMaps(issues);
        attributes = TransportMaps.copy(attributes);
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
                HttpScenarioSuiteMetrics.issueCount(reports),
                resolved.scenarioIds(),
                HttpScenarioSuiteMetrics.scenarioMaps(reports),
                HttpScenarioSuiteMetrics.issues(reports),
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
        return HttpScenarioProjection.suiteReport(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP scenario suite report");
    }
}

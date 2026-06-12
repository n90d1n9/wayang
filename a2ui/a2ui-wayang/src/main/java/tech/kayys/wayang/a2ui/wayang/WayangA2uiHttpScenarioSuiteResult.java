package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpScenarioSuiteMetrics;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.List;
import java.util.Map;

/**
 * Captured result of an HTTP scenario suite run.
 */
public record WayangA2uiHttpScenarioSuiteResult(
        String suiteId,
        List<WayangA2uiHttpScenarioResult> scenarioResults,
        Map<String, Object> attributes) {

    public WayangA2uiHttpScenarioSuiteResult {
        suiteId = RecordValues.textOrDefault(suiteId, "a2ui-http-suite");
        scenarioResults = RecordCollections.nonNullList(scenarioResults);
        attributes = TransportMaps.copy(attributes);
    }

    public int scenarioCount() {
        return HttpScenarioSuiteMetrics.scenarioCount(scenarioResults);
    }

    public long passedScenarioCount() {
        return HttpScenarioSuiteMetrics.passedScenarioCount(scenarioResults);
    }

    public long failedScenarioCount() {
        return HttpScenarioSuiteMetrics.failedScenarioCount(scenarioResults);
    }

    public long exchangeCount() {
        return HttpScenarioSuiteMetrics.exchangeCount(scenarioResults);
    }

    public long successfulCount() {
        return HttpScenarioSuiteMetrics.successfulCount(scenarioResults);
    }

    public long clientErrorCount() {
        return HttpScenarioSuiteMetrics.clientErrorCount(scenarioResults);
    }

    public long serverErrorCount() {
        return HttpScenarioSuiteMetrics.serverErrorCount(scenarioResults);
    }

    public long handledCount() {
        return HttpScenarioSuiteMetrics.handledCount(scenarioResults);
    }

    public long rejectedCount() {
        return HttpScenarioSuiteMetrics.rejectedCount(scenarioResults);
    }

    public boolean hasTransportErrors() {
        return HttpScenarioSuiteMetrics.hasTransportErrors(scenarioResults);
    }

    public long issueCount() {
        return HttpScenarioSuiteMetrics.issueCount(reports());
    }

    public List<String> scenarioIds() {
        return HttpScenarioSuiteMetrics.scenarioIds(scenarioResults);
    }

    public List<WayangA2uiHttpScenarioReport> reports() {
        return HttpScenarioSuiteMetrics.reports(scenarioResults);
    }

    public WayangA2uiHttpScenarioSuiteReport report() {
        return WayangA2uiHttpScenarioSuiteReport.from(this);
    }

    public Map<String, Object> toMap() {
        return report().toMap();
    }

    public String toJson() {
        return report().toJson();
    }

    public WayangA2uiHttpExpectationResult validate(WayangA2uiHttpScenarioSuiteExpectation expectation) {
        WayangA2uiHttpScenarioSuiteExpectation resolved = expectation == null
                ? WayangA2uiHttpScenarioSuiteExpectation.pass()
                : expectation;
        return resolved.validate(this);
    }
}

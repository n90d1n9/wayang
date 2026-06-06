package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Captured result of an HTTP scenario suite run.
 */
public record WayangA2uiHttpScenarioSuiteResult(
        String suiteId,
        List<WayangA2uiHttpScenarioResult> scenarioResults,
        Map<String, Object> attributes) {

    public WayangA2uiHttpScenarioSuiteResult {
        suiteId = suiteId == null || suiteId.isBlank() ? "a2ui-http-suite" : suiteId.trim();
        scenarioResults = scenarioResults == null
                ? List.of()
                : scenarioResults.stream()
                        .filter(Objects::nonNull)
                        .toList();
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public int scenarioCount() {
        return scenarioResults.size();
    }

    public long passedScenarioCount() {
        return scenarioResults.stream()
                .map(WayangA2uiHttpScenarioResult::report)
                .filter(WayangA2uiHttpScenarioReport::passed)
                .count();
    }

    public long failedScenarioCount() {
        return scenarioCount() - passedScenarioCount();
    }

    public long exchangeCount() {
        return scenarioResults.stream()
                .mapToLong(WayangA2uiHttpScenarioResult::exchangeCount)
                .sum();
    }

    public long successfulCount() {
        return scenarioResults.stream()
                .mapToLong(WayangA2uiHttpScenarioResult::successfulCount)
                .sum();
    }

    public long clientErrorCount() {
        return scenarioResults.stream()
                .mapToLong(WayangA2uiHttpScenarioResult::clientErrorCount)
                .sum();
    }

    public long serverErrorCount() {
        return scenarioResults.stream()
                .mapToLong(WayangA2uiHttpScenarioResult::serverErrorCount)
                .sum();
    }

    public long handledCount() {
        return scenarioResults.stream()
                .mapToLong(WayangA2uiHttpScenarioResult::handledCount)
                .sum();
    }

    public long rejectedCount() {
        return scenarioResults.stream()
                .mapToLong(WayangA2uiHttpScenarioResult::rejectedCount)
                .sum();
    }

    public boolean hasTransportErrors() {
        return scenarioResults.stream().anyMatch(WayangA2uiHttpScenarioResult::hasTransportErrors);
    }

    public long issueCount() {
        return reports().stream()
                .mapToLong(WayangA2uiHttpScenarioReport::issueCount)
                .sum();
    }

    public List<String> scenarioIds() {
        return scenarioResults.stream()
                .map(WayangA2uiHttpScenarioResult::scenarioId)
                .toList();
    }

    public List<WayangA2uiHttpScenarioReport> reports() {
        return scenarioResults.stream()
                .map(WayangA2uiHttpScenarioResult::report)
                .toList();
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

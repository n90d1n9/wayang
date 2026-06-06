package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Captured result of an HTTP scenario run.
 */
public record WayangA2uiHttpScenarioResult(
        String scenarioId,
        List<WayangA2uiHttpScenarioExchange> exchanges,
        Map<String, Object> attributes) {

    public WayangA2uiHttpScenarioResult {
        scenarioId = scenarioId == null || scenarioId.isBlank() ? "a2ui-http-scenario" : scenarioId.trim();
        exchanges = exchanges == null
                ? List.of()
                : exchanges.stream()
                        .filter(Objects::nonNull)
                        .toList();
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public int exchangeCount() {
        return exchanges.size();
    }

    public long successfulCount() {
        return exchanges.stream()
                .filter(WayangA2uiHttpScenarioExchange::successful)
                .count();
    }

    public long clientErrorCount() {
        return exchanges.stream()
                .mapToInt(WayangA2uiHttpScenarioExchange::statusCode)
                .filter(statusCode -> statusCode >= 400 && statusCode < 500)
                .count();
    }

    public long serverErrorCount() {
        return exchanges.stream()
                .mapToInt(WayangA2uiHttpScenarioExchange::statusCode)
                .filter(statusCode -> statusCode >= 500)
                .count();
    }

    public long handledCount() {
        return exchanges.stream()
                .map(WayangA2uiHttpScenarioExchange::transportResponse)
                .mapToLong(WayangA2uiTransportResponse::handledCount)
                .sum();
    }

    public long rejectedCount() {
        return exchanges.stream()
                .map(WayangA2uiHttpScenarioExchange::transportResponse)
                .mapToLong(WayangA2uiTransportResponse::rejectedCount)
                .sum();
    }

    public boolean hasTransportErrors() {
        return exchanges.stream().anyMatch(WayangA2uiHttpScenarioExchange::transportError);
    }

    public List<Integer> statusCodes() {
        return exchanges.stream()
                .map(WayangA2uiHttpScenarioExchange::statusCode)
                .toList();
    }

    public List<WayangA2uiTransportOutcome> outcomes() {
        return exchanges.stream()
                .map(WayangA2uiHttpScenarioExchange::outcome)
                .toList();
    }

    public List<Map<String, Object>> responseEnvelopes() {
        return exchanges.stream()
                .map(WayangA2uiHttpScenarioExchange::responseEnvelope)
                .toList();
    }

    public WayangA2uiHttpScenarioReport report() {
        return WayangA2uiHttpScenarioReport.from(this);
    }

    public Map<String, Object> toMap() {
        return report().toMap();
    }

    public String toJson() {
        return report().toJson();
    }

    public WayangA2uiHttpExpectationResult validate(WayangA2uiHttpScenarioExpectation expectation) {
        WayangA2uiHttpScenarioExpectation resolved = expectation == null
                ? WayangA2uiHttpScenarioExpectation.pass()
                : expectation;
        return resolved.validate(this);
    }
}

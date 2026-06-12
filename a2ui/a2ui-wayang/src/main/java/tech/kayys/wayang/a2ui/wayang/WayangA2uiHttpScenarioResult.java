package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpExchangeMetrics;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.List;
import java.util.Map;

/**
 * Captured result of an HTTP scenario run.
 */
public record WayangA2uiHttpScenarioResult(
        String scenarioId,
        List<WayangA2uiHttpScenarioExchange> exchanges,
        Map<String, Object> attributes) {

    public WayangA2uiHttpScenarioResult {
        scenarioId = RecordValues.textOrDefault(scenarioId, "a2ui-http-scenario");
        exchanges = RecordCollections.nonNullList(exchanges);
        attributes = TransportMaps.copy(attributes);
    }

    public int exchangeCount() {
        return HttpExchangeMetrics.exchangeCount(exchanges);
    }

    public long successfulCount() {
        return HttpExchangeMetrics.successfulCount(exchanges);
    }

    public long clientErrorCount() {
        return HttpExchangeMetrics.clientErrorCount(exchanges);
    }

    public long serverErrorCount() {
        return HttpExchangeMetrics.serverErrorCount(exchanges);
    }

    public long handledCount() {
        return HttpExchangeMetrics.handledCount(exchanges);
    }

    public long rejectedCount() {
        return HttpExchangeMetrics.rejectedCount(exchanges);
    }

    public boolean hasTransportErrors() {
        return HttpExchangeMetrics.hasTransportErrors(exchanges);
    }

    public List<Integer> statusCodes() {
        return HttpExchangeMetrics.statusCodes(exchanges);
    }

    public List<WayangA2uiTransportOutcome> outcomes() {
        return HttpExchangeMetrics.outcomes(exchanges);
    }

    public List<Map<String, Object>> responseEnvelopes() {
        return HttpExchangeMetrics.responseEnvelopes(exchanges);
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

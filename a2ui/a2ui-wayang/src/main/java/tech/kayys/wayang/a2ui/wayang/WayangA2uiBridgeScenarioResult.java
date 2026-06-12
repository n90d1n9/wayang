package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportExchangeMetrics;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.List;
import java.util.Map;

/**
 * Captured result of a bridge scenario run.
 */
public record WayangA2uiBridgeScenarioResult(
        String scenarioId,
        List<WayangA2uiBridgeScenarioExchange> exchanges,
        Map<String, Object> attributes) {

    public WayangA2uiBridgeScenarioResult {
        scenarioId = RecordValues.textOrDefault(scenarioId, "a2ui-bridge-scenario");
        exchanges = RecordCollections.nonNullList(exchanges);
        attributes = TransportMaps.copy(attributes);
    }

    public int exchangeCount() {
        return exchanges.size();
    }

    public long handledCount() {
        return TransportExchangeMetrics.handledCount(exchanges);
    }

    public long rejectedCount() {
        return TransportExchangeMetrics.rejectedCount(exchanges);
    }

    public boolean hasTransportErrors() {
        return TransportExchangeMetrics.hasTransportErrors(exchanges);
    }

    public List<WayangA2uiTransportOutcome> outcomes() {
        return TransportExchangeMetrics.outcomes(exchanges);
    }

    public List<Map<String, Object>> responseEnvelopes() {
        return TransportExchangeMetrics.responseEnvelopes(exchanges);
    }
}

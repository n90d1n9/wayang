package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Captured result of a bridge scenario run.
 */
public record WayangA2uiBridgeScenarioResult(
        String scenarioId,
        List<WayangA2uiBridgeScenarioExchange> exchanges,
        Map<String, Object> attributes) {

    public WayangA2uiBridgeScenarioResult {
        scenarioId = scenarioId == null || scenarioId.isBlank() ? "a2ui-bridge-scenario" : scenarioId.trim();
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

    public long handledCount() {
        return exchanges.stream()
                .map(WayangA2uiBridgeScenarioExchange::response)
                .map(WayangA2uiBridgeResponse::transportResponse)
                .mapToLong(WayangA2uiTransportResponse::handledCount)
                .sum();
    }

    public long rejectedCount() {
        return exchanges.stream()
                .map(WayangA2uiBridgeScenarioExchange::response)
                .map(WayangA2uiBridgeResponse::transportResponse)
                .mapToLong(WayangA2uiTransportResponse::rejectedCount)
                .sum();
    }

    public boolean hasTransportErrors() {
        return exchanges.stream().anyMatch(WayangA2uiBridgeScenarioExchange::transportError);
    }

    public List<WayangA2uiTransportOutcome> outcomes() {
        return exchanges.stream()
                .map(WayangA2uiBridgeScenarioExchange::outcome)
                .toList();
    }

    public List<Map<String, Object>> responseEnvelopes() {
        return exchanges.stream()
                .map(WayangA2uiBridgeScenarioExchange::responseEnvelope)
                .toList();
    }
}

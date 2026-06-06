package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Captured result of a mounted A2UI endpoint diagnostics run.
 */
public record WayangA2uiHttpEndpointDiagnosticResult(
        String diagnosticsId,
        List<WayangA2uiHttpEndpointExchange> exchanges,
        Map<String, Object> attributes) {

    public WayangA2uiHttpEndpointDiagnosticResult {
        diagnosticsId = diagnosticsId == null || diagnosticsId.isBlank()
                ? WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID
                : diagnosticsId.trim();
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

    public long knownPathCount() {
        return exchanges.stream()
                .filter(WayangA2uiHttpEndpointExchange::knownPath)
                .count();
    }

    public long unknownPathCount() {
        return exchangeCount() - knownPathCount();
    }

    public long matchedCount() {
        return exchanges.stream()
                .filter(WayangA2uiHttpEndpointExchange::matched)
                .count();
    }

    public long unmatchedCount() {
        return exchangeCount() - matchedCount();
    }

    public long successfulCount() {
        return exchanges.stream()
                .filter(WayangA2uiHttpEndpointExchange::successful)
                .count();
    }

    public long clientErrorCount() {
        return exchanges.stream()
                .mapToInt(WayangA2uiHttpEndpointExchange::statusCode)
                .filter(statusCode -> statusCode >= 400 && statusCode < 500)
                .count();
    }

    public long serverErrorCount() {
        return exchanges.stream()
                .mapToInt(WayangA2uiHttpEndpointExchange::statusCode)
                .filter(statusCode -> statusCode >= 500)
                .count();
    }

    public long handledCount() {
        return exchanges.stream()
                .map(WayangA2uiHttpEndpointExchange::transportResponse)
                .mapToLong(WayangA2uiTransportResponse::handledCount)
                .sum();
    }

    public long rejectedCount() {
        return exchanges.stream()
                .map(WayangA2uiHttpEndpointExchange::transportResponse)
                .mapToLong(WayangA2uiTransportResponse::rejectedCount)
                .sum();
    }

    public boolean hasTransportErrors() {
        return exchanges.stream().anyMatch(WayangA2uiHttpEndpointExchange::transportError);
    }

    public List<Integer> statusCodes() {
        return exchanges.stream()
                .map(WayangA2uiHttpEndpointExchange::statusCode)
                .toList();
    }

    public List<WayangA2uiTransportOutcome> outcomes() {
        return exchanges.stream()
                .map(WayangA2uiHttpEndpointExchange::outcome)
                .toList();
    }

    public List<Map<String, Object>> responseEnvelopes() {
        return exchanges.stream()
                .map(WayangA2uiHttpEndpointExchange::responseEnvelope)
                .toList();
    }

    public WayangA2uiHttpEndpointDiagnosticReport report() {
        return WayangA2uiHttpEndpointDiagnosticReport.from(this);
    }

    public WayangA2uiHttpEndpointDiagnosticSummary summary() {
        return WayangA2uiHttpEndpointDiagnosticSummary.from(this);
    }

    public Map<String, Object> toMap() {
        return report().toMap();
    }

    public String toJson() {
        return report().toJson();
    }
}

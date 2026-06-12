package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpExchangeMetrics;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.List;
import java.util.Map;

/**
 * Captured result of a mounted A2UI endpoint diagnostics run.
 */
public record WayangA2uiHttpEndpointDiagnosticResult(
        String diagnosticsId,
        List<WayangA2uiHttpEndpointExchange> exchanges,
        Map<String, Object> attributes) {

    public WayangA2uiHttpEndpointDiagnosticResult {
        diagnosticsId = RecordValues.textOrDefault(
                diagnosticsId,
                WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID);
        exchanges = RecordCollections.nonNullList(exchanges);
        attributes = TransportMaps.copy(attributes);
    }

    public int exchangeCount() {
        return HttpExchangeMetrics.exchangeCount(exchanges);
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

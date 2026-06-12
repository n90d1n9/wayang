package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpEndpointDiagnosticProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Stable machine-readable projection of mounted A2UI endpoint diagnostics.
 */
public record WayangA2uiHttpEndpointDiagnosticReport(
        String diagnosticsId,
        int exchangeCount,
        long knownPathCount,
        long unknownPathCount,
        long matchedCount,
        long unmatchedCount,
        long successfulCount,
        long clientErrorCount,
        long serverErrorCount,
        long handledCount,
        long rejectedCount,
        boolean transportErrors,
        List<Integer> statusCodes,
        List<String> outcomes,
        List<Map<String, Object>> exchanges,
        List<Map<String, Object>> issues,
        Map<String, Object> attributes) {

    public WayangA2uiHttpEndpointDiagnosticReport {
        diagnosticsId = RecordValues.textOrDefault(
                diagnosticsId,
                WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID);
        exchangeCount = RecordNumbers.nonNegative(exchangeCount);
        knownPathCount = RecordNumbers.nonNegative(knownPathCount);
        unknownPathCount = RecordNumbers.nonNegative(unknownPathCount);
        matchedCount = RecordNumbers.nonNegative(matchedCount);
        unmatchedCount = RecordNumbers.nonNegative(unmatchedCount);
        successfulCount = RecordNumbers.nonNegative(successfulCount);
        clientErrorCount = RecordNumbers.nonNegative(clientErrorCount);
        serverErrorCount = RecordNumbers.nonNegative(serverErrorCount);
        handledCount = RecordNumbers.nonNegative(handledCount);
        rejectedCount = RecordNumbers.nonNegative(rejectedCount);
        statusCodes = RecordCollections.copyList(statusCodes);
        outcomes = DecodeCollections.nonBlankTexts(outcomes);
        exchanges = TransportMaps.copyMaps(exchanges);
        issues = TransportMaps.copyMaps(issues);
        attributes = TransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpEndpointDiagnosticReport from(WayangA2uiHttpEndpointDiagnosticResult result) {
        WayangA2uiHttpEndpointDiagnosticResult resolved = Objects.requireNonNull(result, "result");
        return new WayangA2uiHttpEndpointDiagnosticReport(
                resolved.diagnosticsId(),
                resolved.exchangeCount(),
                resolved.knownPathCount(),
                resolved.unknownPathCount(),
                resolved.matchedCount(),
                resolved.unmatchedCount(),
                resolved.successfulCount(),
                resolved.clientErrorCount(),
                resolved.serverErrorCount(),
                resolved.handledCount(),
                resolved.rejectedCount(),
                resolved.hasTransportErrors(),
                resolved.statusCodes(),
                resolved.outcomes().stream()
                        .map(WayangA2uiTransportOutcome::name)
                        .toList(),
                HttpEndpointDiagnosticProjection.exchanges(resolved.exchanges()),
                HttpEndpointDiagnosticProjection.issues(resolved.diagnosticsId(), resolved.exchanges()),
                resolved.attributes());
    }

    public static WayangA2uiHttpEndpointDiagnosticReport empty() {
        return new WayangA2uiHttpEndpointDiagnosticReport(
                WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID,
                0,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of());
    }

    public static WayangA2uiHttpEndpointDiagnosticReport fromMap(Map<?, ?> values) {
        return WayangA2uiHttpEndpointDiagnosticReportDecoder.fromMap(values);
    }

    public static WayangA2uiHttpEndpointDiagnosticReport fromJson(String json) {
        return WayangA2uiHttpEndpointDiagnosticReportDecoder.fromJson(json);
    }

    public int issueCount() {
        return issues.size();
    }

    public boolean passed() {
        return issueCount() == 0
                && clientErrorCount == 0L
                && serverErrorCount == 0L
                && !transportErrors
                && unknownPathCount == 0L
                && unmatchedCount == 0L;
    }

    public WayangA2uiHttpEndpointDiagnosticSummary summary() {
        return WayangA2uiHttpEndpointDiagnosticSummary.from(this);
    }

    public Map<String, Object> toMap() {
        return HttpEndpointDiagnosticProjection.report(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP endpoint diagnostic report");
    }

}

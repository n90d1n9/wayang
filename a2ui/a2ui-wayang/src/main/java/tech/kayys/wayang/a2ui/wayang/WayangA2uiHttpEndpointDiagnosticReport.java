package tech.kayys.wayang.a2ui.wayang;

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
        diagnosticsId = diagnosticsId == null || diagnosticsId.isBlank()
                ? WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID
                : diagnosticsId.trim();
        exchangeCount = Math.max(0, exchangeCount);
        knownPathCount = Math.max(0L, knownPathCount);
        unknownPathCount = Math.max(0L, unknownPathCount);
        matchedCount = Math.max(0L, matchedCount);
        unmatchedCount = Math.max(0L, unmatchedCount);
        successfulCount = Math.max(0L, successfulCount);
        clientErrorCount = Math.max(0L, clientErrorCount);
        serverErrorCount = Math.max(0L, serverErrorCount);
        handledCount = Math.max(0L, handledCount);
        rejectedCount = Math.max(0L, rejectedCount);
        statusCodes = statusCodes == null ? List.of() : List.copyOf(statusCodes);
        outcomes = outcomes == null
                ? List.of()
                : outcomes.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .toList();
        exchanges = WayangA2uiTransportMaps.copyMaps(exchanges);
        issues = WayangA2uiTransportMaps.copyMaps(issues);
        attributes = WayangA2uiTransportMaps.copy(attributes);
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
                WayangA2uiHttpEndpointDiagnosticProjection.exchanges(resolved.exchanges()),
                WayangA2uiHttpEndpointDiagnosticProjection.issues(resolved.diagnosticsId(), resolved.exchanges()),
                resolved.attributes());
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
        return WayangA2uiHttpEndpointDiagnosticProjection.report(this);
    }

    public String toJson() {
        return WayangA2uiTransportJson.json(toMap(), "Unable to encode A2UI HTTP endpoint diagnostic report");
    }

}

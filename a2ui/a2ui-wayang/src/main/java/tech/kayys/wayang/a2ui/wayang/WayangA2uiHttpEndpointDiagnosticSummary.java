package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;

/**
 * Compact operator-facing summary for mounted A2UI endpoint diagnostics.
 */
public record WayangA2uiHttpEndpointDiagnosticSummary(
        String diagnosticsId,
        boolean passed,
        int exitCode,
        int exchangeCount,
        int issueCount,
        long knownPathCount,
        long unknownPathCount,
        long matchedCount,
        long unmatchedCount,
        long successfulCount,
        long clientErrorCount,
        long serverErrorCount,
        boolean transportErrors,
        List<Integer> statusCodes,
        List<String> outcomes,
        List<String> issueCategories,
        List<String> errorCodes,
        Map<String, Object> attributes) {

    public WayangA2uiHttpEndpointDiagnosticSummary {
        diagnosticsId = diagnosticsId == null || diagnosticsId.isBlank()
                ? WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID
                : diagnosticsId.trim();
        exitCode = Math.max(0, exitCode);
        exchangeCount = Math.max(0, exchangeCount);
        issueCount = Math.max(0, issueCount);
        knownPathCount = Math.max(0L, knownPathCount);
        unknownPathCount = Math.max(0L, unknownPathCount);
        matchedCount = Math.max(0L, matchedCount);
        unmatchedCount = Math.max(0L, unmatchedCount);
        successfulCount = Math.max(0L, successfulCount);
        clientErrorCount = Math.max(0L, clientErrorCount);
        serverErrorCount = Math.max(0L, serverErrorCount);
        statusCodes = statusCodes == null ? List.of() : List.copyOf(statusCodes);
        outcomes = WayangA2uiDecodeCollections.nonBlankTexts(outcomes);
        issueCategories = WayangA2uiDecodeCollections.nonBlankTexts(issueCategories);
        errorCodes = WayangA2uiDecodeCollections.nonBlankTexts(errorCodes);
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpEndpointDiagnosticSummary from(WayangA2uiHttpEndpointDiagnosticResult result) {
        return from(result.report());
    }

    public static WayangA2uiHttpEndpointDiagnosticSummary from(WayangA2uiHttpEndpointDiagnosticReport report) {
        WayangA2uiHttpEndpointDiagnosticReport resolved =
                report == null ? WayangA2uiHttpEndpointDiagnosticReport.fromMap(Map.of()) : report;
        boolean passed = resolved.passed();
        return new WayangA2uiHttpEndpointDiagnosticSummary(
                resolved.diagnosticsId(),
                passed,
                passed ? WayangA2uiHttpSmokeResult.EXIT_SUCCESS : WayangA2uiHttpSmokeResult.EXIT_FAILURE,
                resolved.exchangeCount(),
                resolved.issueCount(),
                resolved.knownPathCount(),
                resolved.unknownPathCount(),
                resolved.matchedCount(),
                resolved.unmatchedCount(),
                resolved.successfulCount(),
                resolved.clientErrorCount(),
                resolved.serverErrorCount(),
                resolved.transportErrors(),
                resolved.statusCodes(),
                resolved.outcomes(),
                WayangA2uiHttpEndpointDiagnosticProjection.issueValues(resolved.issues(), "category"),
                WayangA2uiHttpEndpointDiagnosticProjection.issueValues(resolved.issues(), "errorCode"),
                resolved.attributes());
    }

    public static WayangA2uiHttpEndpointDiagnosticSummary fromMap(Map<?, ?> values) {
        return WayangA2uiHttpEndpointDiagnosticSummaryDecoder.fromMap(values);
    }

    public static WayangA2uiHttpEndpointDiagnosticSummary fromJson(String json) {
        return WayangA2uiHttpEndpointDiagnosticSummaryDecoder.fromJson(json);
    }

    public boolean successfulExit() {
        return passed && exitCode == WayangA2uiHttpSmokeResult.EXIT_SUCCESS;
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpEndpointDiagnosticProjection.summary(this);
    }

    public String toJson() {
        return WayangA2uiTransportJson.json(toMap(), "Unable to encode A2UI HTTP endpoint diagnostic summary");
    }

}

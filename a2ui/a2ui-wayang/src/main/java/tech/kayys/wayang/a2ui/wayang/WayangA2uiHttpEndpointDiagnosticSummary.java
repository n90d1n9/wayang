package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpEndpointDiagnosticProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpIssueMaps;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

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
        diagnosticsId = RecordValues.textOrDefault(
                diagnosticsId,
                WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID);
        exitCode = RecordNumbers.nonNegative(exitCode);
        exchangeCount = RecordNumbers.nonNegative(exchangeCount);
        issueCount = RecordNumbers.nonNegative(issueCount);
        knownPathCount = RecordNumbers.nonNegative(knownPathCount);
        unknownPathCount = RecordNumbers.nonNegative(unknownPathCount);
        matchedCount = RecordNumbers.nonNegative(matchedCount);
        unmatchedCount = RecordNumbers.nonNegative(unmatchedCount);
        successfulCount = RecordNumbers.nonNegative(successfulCount);
        clientErrorCount = RecordNumbers.nonNegative(clientErrorCount);
        serverErrorCount = RecordNumbers.nonNegative(serverErrorCount);
        statusCodes = RecordCollections.copyList(statusCodes);
        outcomes = DecodeCollections.nonBlankTexts(outcomes);
        issueCategories = DecodeCollections.nonBlankTexts(issueCategories);
        errorCodes = DecodeCollections.nonBlankTexts(errorCodes);
        attributes = TransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpEndpointDiagnosticSummary from(WayangA2uiHttpEndpointDiagnosticResult result) {
        return from(result.report());
    }

    public static WayangA2uiHttpEndpointDiagnosticSummary empty() {
        return from(WayangA2uiHttpEndpointDiagnosticReport.empty());
    }

    public static WayangA2uiHttpEndpointDiagnosticSummary from(WayangA2uiHttpEndpointDiagnosticReport report) {
        if (report == null) {
            return empty();
        }
        WayangA2uiHttpEndpointDiagnosticReport resolved = report;
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
                HttpIssueMaps.issueValues(resolved.issues(), "category"),
                HttpIssueMaps.issueValues(resolved.issues(), "errorCode"),
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
        return HttpEndpointDiagnosticProjection.summary(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP endpoint diagnostic summary");
    }

}

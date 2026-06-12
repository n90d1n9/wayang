package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticIssue;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticReport;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticRun;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointDiagnosticSummary;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointExchange;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2UI HTTP endpoint diagnostic reports.
 */
public final class HttpEndpointDiagnosticProjection {

    private HttpEndpointDiagnosticProjection() {
    }

    public static Map<String, Object> report(WayangA2uiHttpEndpointDiagnosticReport report) {
        WayangA2uiHttpEndpointDiagnosticReport resolved = Objects.requireNonNull(report, "report");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("diagnosticsId", resolved.diagnosticsId());
        values.put(WayangA2uiTransportFields.PASSED, resolved.passed());
        values.put("exchangeCount", resolved.exchangeCount());
        values.put("knownPathCount", resolved.knownPathCount());
        values.put("unknownPathCount", resolved.unknownPathCount());
        values.put("matchedCount", resolved.matchedCount());
        values.put("unmatchedCount", resolved.unmatchedCount());
        HttpReportMetrics.putOutcomeCounts(
                values,
                resolved.successfulCount(),
                resolved.clientErrorCount(),
                resolved.serverErrorCount());
        HttpReportMetrics.putTransportCounts(
                values,
                resolved.handledCount(),
                resolved.rejectedCount());
        HttpReportMetrics.putTransportDigest(
                values,
                resolved.transportErrors(),
                resolved.statusCodes(),
                resolved.outcomes());
        values.put("attributes", resolved.attributes());
        values.put("exchanges", resolved.exchanges());
        values.put(WayangA2uiTransportFields.ISSUE_COUNT, resolved.issueCount());
        values.put("issues", resolved.issues());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> run(WayangA2uiHttpEndpointDiagnosticRun run) {
        WayangA2uiHttpEndpointDiagnosticRun resolved = Objects.requireNonNull(run, "run");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("summary", resolved.summary().toMap());
        values.put("report", resolved.report().toMap());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> summary(WayangA2uiHttpEndpointDiagnosticSummary summary) {
        WayangA2uiHttpEndpointDiagnosticSummary resolved = Objects.requireNonNull(summary, "summary");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("diagnosticsId", resolved.diagnosticsId());
        values.put(WayangA2uiTransportFields.PASSED, resolved.passed());
        values.put(WayangA2uiTransportFields.EXIT_CODE, resolved.exitCode());
        values.put("successfulExit", resolved.successfulExit());
        values.put("exchangeCount", resolved.exchangeCount());
        values.put(WayangA2uiTransportFields.ISSUE_COUNT, resolved.issueCount());
        values.put("knownPathCount", resolved.knownPathCount());
        values.put("unknownPathCount", resolved.unknownPathCount());
        values.put("matchedCount", resolved.matchedCount());
        values.put("unmatchedCount", resolved.unmatchedCount());
        HttpReportMetrics.putOutcomeCounts(
                values,
                resolved.successfulCount(),
                resolved.clientErrorCount(),
                resolved.serverErrorCount());
        HttpReportMetrics.putTransportDigest(
                values,
                resolved.transportErrors(),
                resolved.statusCodes(),
                resolved.outcomes());
        values.put("issueCategories", resolved.issueCategories());
        values.put("errorCodes", resolved.errorCodes());
        values.put("attributes", resolved.attributes());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> issue(WayangA2uiHttpEndpointDiagnosticIssue issue) {
        WayangA2uiHttpEndpointDiagnosticIssue resolved = Objects.requireNonNull(issue, "issue");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("diagnosticsId", resolved.diagnosticsId());
        values.put("exchangeIndex", resolved.exchangeIndex());
        values.put("method", resolved.method());
        values.put("path", resolved.path());
        values.put("knownPath", resolved.knownPath());
        values.put("matched", resolved.matched());
        values.put("statusCode", resolved.statusCode());
        values.put("routeOperation", resolved.routeOperation());
        values.put("allow", resolved.allow());
        values.put("outcome", resolved.outcome());
        values.put("category", resolved.category());
        values.put("errorCode", resolved.errorCode());
        values.put("message", resolved.message());
        values.put("attributes", resolved.attributes());
        return TransportMaps.freeze(values);
    }

    public static List<Map<String, Object>> exchanges(List<WayangA2uiHttpEndpointExchange> exchanges) {
        List<WayangA2uiHttpEndpointExchange> resolved = RecordCollections.nonNullList(exchanges);
        List<Map<String, Object>> values = new ArrayList<>();
        for (int index = 0; index < resolved.size(); index++) {
            values.add(exchange(index + 1, resolved.get(index)));
        }
        return List.copyOf(values);
    }

    public static List<Map<String, Object>> issues(
            String diagnosticsId,
            List<WayangA2uiHttpEndpointExchange> exchanges) {
        List<WayangA2uiHttpEndpointExchange> resolved = RecordCollections.nonNullList(exchanges);
        List<Map<String, Object>> values = new ArrayList<>();
        for (int index = 0; index < resolved.size(); index++) {
            WayangA2uiHttpEndpointDiagnosticIssue.from(diagnosticsId, index + 1, resolved.get(index))
                    .map(WayangA2uiHttpEndpointDiagnosticIssue::toMap)
                    .ifPresent(values::add);
        }
        return List.copyOf(values);
    }

    private static Map<String, Object> exchange(int index, WayangA2uiHttpEndpointExchange exchange) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("index", Math.max(1, index));
        values.putAll(Objects.requireNonNull(exchange, "exchange").toMap());
        return TransportMaps.freeze(values);
    }
}

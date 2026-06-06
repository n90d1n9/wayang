package tech.kayys.wayang.a2ui.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Ordered projection helpers for A2UI HTTP endpoint diagnostic reports.
 */
final class WayangA2uiHttpEndpointDiagnosticProjection {

    private WayangA2uiHttpEndpointDiagnosticProjection() {
    }

    static Map<String, Object> report(WayangA2uiHttpEndpointDiagnosticReport report) {
        WayangA2uiHttpEndpointDiagnosticReport resolved = Objects.requireNonNull(report, "report");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("diagnosticsId", resolved.diagnosticsId());
        values.put(WayangA2uiTransportFields.PASSED, resolved.passed());
        values.put("exchangeCount", resolved.exchangeCount());
        values.put("knownPathCount", resolved.knownPathCount());
        values.put("unknownPathCount", resolved.unknownPathCount());
        values.put("matchedCount", resolved.matchedCount());
        values.put("unmatchedCount", resolved.unmatchedCount());
        values.put("successfulCount", resolved.successfulCount());
        values.put("clientErrorCount", resolved.clientErrorCount());
        values.put("serverErrorCount", resolved.serverErrorCount());
        values.put("handledCount", resolved.handledCount());
        values.put("rejectedCount", resolved.rejectedCount());
        values.put("transportErrors", resolved.transportErrors());
        values.put("statusCodes", resolved.statusCodes());
        values.put("outcomes", resolved.outcomes());
        values.put("attributes", resolved.attributes());
        values.put("exchanges", resolved.exchanges());
        values.put(WayangA2uiTransportFields.ISSUE_COUNT, resolved.issueCount());
        values.put("issues", resolved.issues());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> run(WayangA2uiHttpEndpointDiagnosticRun run) {
        WayangA2uiHttpEndpointDiagnosticRun resolved = Objects.requireNonNull(run, "run");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("summary", resolved.summary().toMap());
        values.put("report", resolved.report().toMap());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> summary(WayangA2uiHttpEndpointDiagnosticSummary summary) {
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
        values.put("successfulCount", resolved.successfulCount());
        values.put("clientErrorCount", resolved.clientErrorCount());
        values.put("serverErrorCount", resolved.serverErrorCount());
        values.put("transportErrors", resolved.transportErrors());
        values.put("statusCodes", resolved.statusCodes());
        values.put("outcomes", resolved.outcomes());
        values.put("issueCategories", resolved.issueCategories());
        values.put("errorCodes", resolved.errorCodes());
        values.put("attributes", resolved.attributes());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> issue(WayangA2uiHttpEndpointDiagnosticIssue issue) {
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
        return WayangA2uiTransportMaps.freeze(values);
    }

    static List<Map<String, Object>> exchanges(List<WayangA2uiHttpEndpointExchange> exchanges) {
        List<WayangA2uiHttpEndpointExchange> resolved = exchanges == null ? List.of() : exchanges;
        List<Map<String, Object>> values = new ArrayList<>();
        for (int index = 0; index < resolved.size(); index++) {
            values.add(exchange(index + 1, resolved.get(index)));
        }
        return List.copyOf(values);
    }

    static List<Map<String, Object>> issues(
            String diagnosticsId,
            List<WayangA2uiHttpEndpointExchange> exchanges) {
        List<WayangA2uiHttpEndpointExchange> resolved = exchanges == null ? List.of() : exchanges;
        List<Map<String, Object>> values = new ArrayList<>();
        for (int index = 0; index < resolved.size(); index++) {
            WayangA2uiHttpEndpointDiagnosticIssue.from(diagnosticsId, index + 1, resolved.get(index))
                    .map(WayangA2uiHttpEndpointDiagnosticIssue::toMap)
                    .ifPresent(values::add);
        }
        return List.copyOf(values);
    }

    static List<String> issueValues(List<Map<String, Object>> issues, String key) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (Map<String, Object> issue : issues) {
            String value = WayangA2uiDecodeValues.text(issue == null ? null : issue.get(key));
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private static Map<String, Object> exchange(int index, WayangA2uiHttpEndpointExchange exchange) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("index", Math.max(1, index));
        values.putAll(Objects.requireNonNull(exchange, "exchange").toMap());
        return WayangA2uiTransportMaps.freeze(values);
    }
}

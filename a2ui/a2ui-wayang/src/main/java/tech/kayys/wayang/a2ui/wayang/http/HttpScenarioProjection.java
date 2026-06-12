package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRequest;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarioExchange;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarioIssue;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarioReport;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarioSuiteReport;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportError;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportResponse;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projections for A2UI HTTP scenario harness reports.
 */
public final class HttpScenarioProjection {

    private HttpScenarioProjection() {
    }

    public static Map<String, Object> report(WayangA2uiHttpScenarioReport report) {
        WayangA2uiHttpScenarioReport resolved = Objects.requireNonNull(report, "report");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("scenarioId", resolved.scenarioId());
        values.put("passed", resolved.passed());
        values.put("exchangeCount", resolved.exchangeCount());
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
        values.put("issueCount", resolved.issueCount());
        values.put("issues", resolved.issues());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> suiteReport(WayangA2uiHttpScenarioSuiteReport report) {
        WayangA2uiHttpScenarioSuiteReport resolved = Objects.requireNonNull(report, "report");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("suiteId", resolved.suiteId());
        values.put("passed", resolved.passed());
        values.put("scenarioCount", resolved.scenarioCount());
        values.put("passedScenarioCount", resolved.passedScenarioCount());
        values.put("failedScenarioCount", resolved.failedScenarioCount());
        values.put("exchangeCount", resolved.exchangeCount());
        HttpReportMetrics.putOutcomeCounts(
                values,
                resolved.successfulCount(),
                resolved.clientErrorCount(),
                resolved.serverErrorCount());
        HttpReportMetrics.putTransportCounts(
                values,
                resolved.handledCount(),
                resolved.rejectedCount());
        HttpReportMetrics.putTransportErrorFlag(values, resolved.transportErrors());
        values.put("issueCount", resolved.issueCount());
        values.put("scenarioIds", resolved.scenarioIds());
        values.put("attributes", resolved.attributes());
        values.put("scenarios", resolved.scenarios());
        values.put("issues", resolved.issues());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> issue(WayangA2uiHttpScenarioIssue issue) {
        WayangA2uiHttpScenarioIssue resolved = Objects.requireNonNull(issue, "issue");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("scenarioId", resolved.scenarioId());
        values.put("exchangeIndex", resolved.exchangeIndex());
        values.put("method", resolved.method());
        values.put("path", resolved.path());
        values.put("statusCode", resolved.statusCode());
        values.put("routeOperation", resolved.routeOperation());
        values.put("outcome", resolved.outcome());
        values.put("errorCode", resolved.errorCode());
        values.put("message", resolved.message());
        values.put("attributes", resolved.attributes());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> exchange(WayangA2uiHttpScenarioExchange exchange) {
        WayangA2uiHttpScenarioExchange resolved = Objects.requireNonNull(exchange, "exchange");
        WayangA2uiTransportResponse transportResponse = resolved.transportResponse();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("index", resolved.index());
        values.put("request", request(resolved.request()));
        values.put("response", response(resolved.response(), transportResponse));
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> request(WayangA2uiHttpRequest request) {
        WayangA2uiHttpRequest resolved = Objects.requireNonNull(request, "request");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("method", resolved.method());
        values.put("path", resolved.path());
        values.put("contentType", resolved.contentType());
        values.put("accept", resolved.accept());
        values.put("bodyPresent", !resolved.body().isBlank());
        values.put("bodyLength", resolved.body().length());
        values.put("headers", resolved.headers());
        values.put("attributes", resolved.attributes());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> response(
            WayangA2uiHttpResponse response,
            WayangA2uiTransportResponse transportResponse) {
        WayangA2uiHttpResponse resolved = Objects.requireNonNull(response, "response");
        WayangA2uiTransportResponse resolvedTransport = Objects.requireNonNull(transportResponse, "transportResponse");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", resolved.statusCode());
        values.put("contentType", resolved.contentType());
        values.put("successful", resolved.successful());
        values.put("routeOperation", resolved.header(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION));
        values.put("allow", resolved.header(WayangA2uiHttpResponse.HEADER_ALLOW));
        values.put("outcome", resolvedTransport.outcome().name());
        values.put("transportError", resolvedTransport.transportError().isPresent());
        resolvedTransport.transportError()
                .map(WayangA2uiTransportError::toMap)
                .ifPresent(error -> values.put("transportErrorDetail", error));
        values.put("headers", resolved.headers());
        values.put("responseEnvelope", resolvedTransport.toMap());
        return TransportMaps.freeze(values);
    }
}

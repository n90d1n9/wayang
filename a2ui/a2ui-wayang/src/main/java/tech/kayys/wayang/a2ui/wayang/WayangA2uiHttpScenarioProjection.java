package tech.kayys.wayang.a2ui.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projections for A2UI HTTP scenario harness reports.
 */
final class WayangA2uiHttpScenarioProjection {

    private WayangA2uiHttpScenarioProjection() {
    }

    static Map<String, Object> report(WayangA2uiHttpScenarioReport report) {
        WayangA2uiHttpScenarioReport resolved = Objects.requireNonNull(report, "report");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("scenarioId", resolved.scenarioId());
        values.put("passed", resolved.passed());
        values.put("exchangeCount", resolved.exchangeCount());
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
        values.put("issueCount", resolved.issueCount());
        values.put("issues", resolved.issues());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> suiteReport(WayangA2uiHttpScenarioSuiteReport report) {
        WayangA2uiHttpScenarioSuiteReport resolved = Objects.requireNonNull(report, "report");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("suiteId", resolved.suiteId());
        values.put("passed", resolved.passed());
        values.put("scenarioCount", resolved.scenarioCount());
        values.put("passedScenarioCount", resolved.passedScenarioCount());
        values.put("failedScenarioCount", resolved.failedScenarioCount());
        values.put("exchangeCount", resolved.exchangeCount());
        values.put("successfulCount", resolved.successfulCount());
        values.put("clientErrorCount", resolved.clientErrorCount());
        values.put("serverErrorCount", resolved.serverErrorCount());
        values.put("handledCount", resolved.handledCount());
        values.put("rejectedCount", resolved.rejectedCount());
        values.put("transportErrors", resolved.transportErrors());
        values.put("issueCount", resolved.issueCount());
        values.put("scenarioIds", resolved.scenarioIds());
        values.put("attributes", resolved.attributes());
        values.put("scenarios", resolved.scenarios());
        values.put("issues", resolved.issues());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> issue(WayangA2uiHttpScenarioIssue issue) {
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
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> exchange(WayangA2uiHttpScenarioExchange exchange) {
        WayangA2uiHttpScenarioExchange resolved = Objects.requireNonNull(exchange, "exchange");
        WayangA2uiTransportResponse transportResponse = resolved.transportResponse();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("index", resolved.index());
        values.put("request", request(resolved.request()));
        values.put("response", response(resolved.response(), transportResponse));
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> request(WayangA2uiHttpRequest request) {
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
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> response(
            WayangA2uiHttpResponse response,
            WayangA2uiTransportResponse transportResponse) {
        WayangA2uiHttpResponse resolved = Objects.requireNonNull(response, "response");
        WayangA2uiTransportResponse resolvedTransport = Objects.requireNonNull(transportResponse, "transportResponse");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", resolved.statusCode());
        values.put("contentType", resolved.contentType());
        values.put("successful", resolved.successful());
        values.put("routeOperation", stringHeader(resolved, WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION));
        values.put("allow", stringHeader(resolved, WayangA2uiHttpResponse.HEADER_ALLOW));
        values.put("outcome", resolvedTransport.outcome().name());
        values.put("transportError", resolvedTransport.transportError().isPresent());
        resolvedTransport.transportError()
                .map(WayangA2uiTransportError::toMap)
                .ifPresent(error -> values.put("transportErrorDetail", error));
        values.put("headers", resolved.headers());
        values.put("responseEnvelope", resolvedTransport.toMap());
        return WayangA2uiTransportMaps.freeze(values);
    }

    private static String stringHeader(WayangA2uiHttpResponse response, String headerName) {
        Object value = response.headers().get(headerName);
        return value == null ? "" : String.valueOf(value);
    }
}

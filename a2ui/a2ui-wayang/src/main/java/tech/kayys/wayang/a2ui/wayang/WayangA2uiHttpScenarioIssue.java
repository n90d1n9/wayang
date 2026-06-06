package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;
import java.util.Optional;

/**
 * Compact issue projection for failed HTTP scenario exchanges.
 */
public record WayangA2uiHttpScenarioIssue(
        String scenarioId,
        int exchangeIndex,
        String method,
        String path,
        int statusCode,
        String routeOperation,
        String outcome,
        String errorCode,
        String message,
        Map<String, Object> attributes) {

    public WayangA2uiHttpScenarioIssue {
        scenarioId = scenarioId == null || scenarioId.isBlank() ? "a2ui-http-scenario" : scenarioId.trim();
        exchangeIndex = Math.max(1, exchangeIndex);
        method = WayangA2uiHttpRequest.normalizeMethod(method);
        path = WayangA2uiHttpRequest.normalizePath(path);
        statusCode = Math.max(0, statusCode);
        routeOperation = routeOperation == null ? "" : routeOperation.trim();
        outcome = outcome == null ? "" : outcome.trim();
        errorCode = errorCode == null || errorCode.isBlank() ? "http_exchange_issue" : errorCode.trim();
        message = message == null || message.isBlank() ? defaultMessage(method, path, statusCode) : message.trim();
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static Optional<WayangA2uiHttpScenarioIssue> from(
            String scenarioId,
            WayangA2uiHttpScenarioExchange exchange) {
        if (exchange == null || (exchange.successful() && !exchange.transportError())) {
            return Optional.empty();
        }
        WayangA2uiHttpRequest request = exchange.request();
        WayangA2uiHttpResponse response = exchange.response();
        WayangA2uiTransportResponse transportResponse = exchange.transportResponse();
        Optional<WayangA2uiTransportError> transportError = transportResponse.transportError();
        String code = transportError
                .map(WayangA2uiTransportError::code)
                .orElse("http_status_" + response.statusCode());
        String message = transportError
                .map(WayangA2uiTransportError::message)
                .orElse(defaultMessage(request.method(), request.path(), response.statusCode()));
        return Optional.of(new WayangA2uiHttpScenarioIssue(
                scenarioId,
                exchange.index(),
                request.method(),
                request.path(),
                response.statusCode(),
                stringHeader(response, WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION),
                transportResponse.outcome().name(),
                code,
                message,
                request.attributes()));
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpScenarioProjection.issue(this);
    }

    private static String defaultMessage(String method, String path, int statusCode) {
        return "HTTP status " + statusCode + " for "
                + WayangA2uiHttpRequest.normalizeMethod(method) + " "
                + WayangA2uiHttpRequest.normalizePath(path);
    }

    private static String stringHeader(WayangA2uiHttpResponse response, String headerName) {
        Object value = response.headers().get(headerName);
        return value == null ? "" : String.valueOf(value);
    }
}

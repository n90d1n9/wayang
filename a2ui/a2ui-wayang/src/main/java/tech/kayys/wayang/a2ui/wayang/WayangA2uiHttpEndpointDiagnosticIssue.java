package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;
import java.util.Optional;

/**
 * Compact issue projection for failed mounted endpoint diagnostic exchanges.
 */
public record WayangA2uiHttpEndpointDiagnosticIssue(
        String diagnosticsId,
        int exchangeIndex,
        String method,
        String path,
        boolean knownPath,
        boolean matched,
        int statusCode,
        String routeOperation,
        String allow,
        String outcome,
        String category,
        String errorCode,
        String message,
        Map<String, Object> attributes) {

    public WayangA2uiHttpEndpointDiagnosticIssue {
        diagnosticsId = diagnosticsId == null || diagnosticsId.isBlank()
                ? WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID
                : diagnosticsId.trim();
        exchangeIndex = Math.max(1, exchangeIndex);
        method = WayangA2uiHttpRequest.normalizeMethod(method);
        path = WayangA2uiHttpRequest.normalizePath(path);
        statusCode = Math.max(0, statusCode);
        routeOperation = routeOperation == null ? "" : routeOperation.trim();
        allow = allow == null ? "" : allow.trim();
        outcome = outcome == null ? "" : outcome.trim();
        category = WayangA2uiHttpEndpointDiagnosticIssueCatalog.normalizeCategory(category);
        errorCode = WayangA2uiHttpEndpointDiagnosticIssueCatalog.normalizeErrorCode(errorCode);
        message = message == null || message.isBlank()
                ? defaultMessage(method, path, statusCode)
                : message.trim();
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static Optional<WayangA2uiHttpEndpointDiagnosticIssue> from(
            String diagnosticsId,
            int exchangeIndex,
            WayangA2uiHttpEndpointExchange exchange) {
        if (exchange == null || (exchange.successful()
                && !exchange.transportError()
                && exchange.knownPath()
                && exchange.matched())) {
            return Optional.empty();
        }
        WayangA2uiHttpEndpointRequest request = exchange.request();
        WayangA2uiTransportResponse transport = exchange.transportResponse();
        Optional<WayangA2uiTransportError> transportError = transport.transportError();
        return Optional.of(new WayangA2uiHttpEndpointDiagnosticIssue(
                diagnosticsId,
                exchangeIndex,
                request.method(),
                request.path(),
                exchange.knownPath(),
                exchange.matched(),
                exchange.statusCode(),
                request.operation().orElse(""),
                request.allow().orElse(""),
                transport.outcome().name(),
                category(exchange, transportError),
                errorCode(exchange, transportError),
                message(exchange, transportError),
                request.attributes()));
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpEndpointDiagnosticProjection.issue(this);
    }

    public String toJson() {
        return WayangA2uiTransportJson.json(toMap(), "Unable to encode A2UI HTTP endpoint diagnostic issue");
    }

    private static String category(
            WayangA2uiHttpEndpointExchange exchange,
            Optional<WayangA2uiTransportError> transportError) {
        return WayangA2uiHttpEndpointDiagnosticIssueCatalog.category(
                exchange.knownPath(),
                exchange.matched(),
                transportError.isPresent());
    }

    private static String errorCode(
            WayangA2uiHttpEndpointExchange exchange,
            Optional<WayangA2uiTransportError> transportError) {
        return transportError
                .map(WayangA2uiTransportError::code)
                .filter(code -> !code.isBlank())
                .orElseGet(() -> WayangA2uiHttpEndpointDiagnosticIssueCatalog.fallbackErrorCode(
                        exchange.knownPath(),
                        exchange.matched(),
                        exchange.statusCode()));
    }

    private static String message(
            WayangA2uiHttpEndpointExchange exchange,
            Optional<WayangA2uiTransportError> transportError) {
        return transportError
                .map(WayangA2uiTransportError::message)
                .filter(message -> !message.isBlank())
                .orElseGet(() -> defaultMessage(
                        exchange.request().method(),
                        exchange.request().path(),
                        exchange.statusCode()));
    }

    private static String defaultMessage(String method, String path, int statusCode) {
        return "Endpoint diagnostics issue for "
                + WayangA2uiHttpRequest.normalizeMethod(method) + " "
                + WayangA2uiHttpRequest.normalizePath(path) + " with status "
                + Math.max(0, statusCode) + ".";
    }
}

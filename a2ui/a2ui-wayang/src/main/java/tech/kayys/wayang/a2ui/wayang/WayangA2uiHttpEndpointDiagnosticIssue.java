package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpEndpointDiagnosticProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;

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
        diagnosticsId = RecordValues.textOrDefault(
                diagnosticsId,
                WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID);
        exchangeIndex = RecordNumbers.oneBased(exchangeIndex);
        method = WayangA2uiHttpRequest.normalizeMethod(method);
        path = WayangA2uiHttpRequest.normalizePath(path);
        statusCode = RecordNumbers.nonNegative(statusCode);
        routeOperation = RecordValues.text(routeOperation);
        allow = RecordValues.text(allow);
        outcome = RecordValues.text(outcome);
        category = WayangA2uiHttpEndpointDiagnosticIssueCatalog.normalizeCategory(category);
        errorCode = WayangA2uiHttpEndpointDiagnosticIssueCatalog.normalizeErrorCode(errorCode);
        message = RecordValues.textOrDefault(message, defaultMessage(method, path, statusCode));
        attributes = TransportMaps.copy(attributes);
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
        return HttpEndpointDiagnosticProjection.issue(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP endpoint diagnostic issue");
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
                + RecordNumbers.nonNegative(statusCode) + ".";
    }
}

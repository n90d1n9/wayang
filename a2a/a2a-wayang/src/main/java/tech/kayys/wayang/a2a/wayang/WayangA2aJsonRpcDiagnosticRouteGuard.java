package tech.kayys.wayang.a2a.wayang;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Shared GET/Accept validation for JSON-RPC diagnostic HTTP routes.
 */
final class WayangA2aJsonRpcDiagnosticRouteGuard {

    private WayangA2aJsonRpcDiagnosticRouteGuard() {
    }

    static WayangA2aHttpResponse dispatchGetJson(
            WayangA2aHttpRequest request,
            String path,
            String allow,
            String operation,
            String routeName,
            Supplier<WayangA2aHttpResponse> responseSupplier) {
        Optional<WayangA2aHttpResponse> invalid = validateGetJson(request, path, allow, operation, routeName);
        if (invalid.isPresent()) {
            return invalid.orElseThrow();
        }
        WayangA2aHttpResponse response = Objects.requireNonNull(responseSupplier, "responseSupplier").get();
        return Objects.requireNonNull(response, "response")
                .withHeaders(WayangA2aJsonRpcHttpResponses.routeHeaders(operation, allow));
    }

    static Optional<WayangA2aHttpResponse> validateGetJson(
            WayangA2aHttpRequest request,
            String path,
            String allow,
            String operation,
            String routeName) {
        if (!request.method("GET")) {
            return Optional.of(error(
                    405,
                    "method_not_allowed",
                    "A2A JSON-RPC " + routeName + " path " + path + " requires GET.",
                    allow,
                    operation));
        }
        if (!request.accepts(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)) {
            String actual = request.accept().isBlank() ? "none" : request.accept();
            return Optional.of(error(
                    406,
                    "not_acceptable",
                    "A2A JSON-RPC " + routeName + " path " + path + " produces "
                            + WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON + ", but Accept was " + actual + ".",
                    allow,
                    operation));
        }
        return Optional.empty();
    }

    private static WayangA2aHttpResponse error(
            int status,
            String code,
            String message,
            String allow,
            String operation) {
        return WayangA2aJsonRpcHttpResponses.error(status, operation, allow, code, message);
    }
}

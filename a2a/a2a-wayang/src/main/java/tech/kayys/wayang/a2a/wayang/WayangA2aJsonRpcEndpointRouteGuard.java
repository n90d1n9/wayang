package tech.kayys.wayang.a2a.wayang;

import java.util.Objects;
import java.util.Optional;

/**
 * Shared HTTP validation for the JSON-RPC endpoint route.
 */
final class WayangA2aJsonRpcEndpointRouteGuard {

    private WayangA2aJsonRpcEndpointRouteGuard() {
    }

    static Optional<WayangA2aHttpResponse> validate(
            WayangA2aHttpRequest request,
            WayangA2aJsonRpcHttpRouteDescriptor route,
            WayangA2aJsonRpcHttpRequestContext context) {
        WayangA2aHttpRequest resolved = Objects.requireNonNull(request, "request");
        WayangA2aJsonRpcHttpRouteDescriptor resolvedRoute = Objects.requireNonNull(route, "route");
        WayangA2aJsonRpcHttpRequestContext resolvedContext = Objects.requireNonNull(context, "context");
        if (!resolved.method(resolvedRoute.httpMethod())) {
            return Optional.of(error(
                    405,
                    "method_not_allowed",
                    "A2A JSON-RPC endpoint " + resolvedRoute.path() + " requires "
                            + resolvedRoute.httpMethod() + ".",
                    resolvedRoute,
                    resolvedContext.methodOr(resolvedRoute.operation())));
        }
        if (resolved.body().isBlank()) {
            return Optional.of(error(
                    400,
                    "missing_request_body",
                    "A2A JSON-RPC endpoint " + resolvedRoute.path() + " requires a request body.",
                    resolvedRoute,
                    resolvedRoute.operation()));
        }
        if (!resolved.jsonContentType()) {
            return Optional.of(error(
                    415,
                    "unsupported_media_type",
                    "A2A JSON-RPC endpoint " + resolvedRoute.path()
                            + " requires Content-Type application/json.",
                    resolvedRoute,
                    resolvedContext.methodOr(resolvedRoute.operation())));
        }
        String expectedContentType = resolvedContext.responseMediaType();
        if (!resolved.accepts(expectedContentType)) {
            String actual = resolved.accept().isBlank() ? "none" : resolved.accept();
            return Optional.of(error(
                    406,
                    "not_acceptable",
                    "A2A JSON-RPC endpoint " + resolvedRoute.path() + " produces " + expectedContentType
                            + ", but Accept was " + actual + ".",
                    resolvedRoute,
                    resolvedContext.methodOr(resolvedRoute.operation())));
        }
        return Optional.empty();
    }

    private static WayangA2aHttpResponse error(
            int status,
            String code,
            String message,
            WayangA2aJsonRpcHttpRouteDescriptor route,
            String operation) {
        return WayangA2aJsonRpcHttpResponses.error(status, operation, route.allow(), code, message);
    }
}

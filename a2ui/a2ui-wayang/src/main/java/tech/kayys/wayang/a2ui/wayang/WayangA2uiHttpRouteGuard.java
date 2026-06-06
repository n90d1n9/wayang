package tech.kayys.wayang.a2ui.wayang;

import java.util.Objects;
import java.util.Optional;

/**
 * Reusable HTTP route guard for dependency-free A2UI bindings.
 */
public final class WayangA2uiHttpRouteGuard {

    private static final WayangA2uiHttpRouteGuard STRICT = new WayangA2uiHttpRouteGuard();

    public static WayangA2uiHttpRouteGuard strict() {
        return STRICT;
    }

    public Optional<WayangA2uiHttpResponse> validate(WayangA2uiHttpRequest request, WayangA2uiHttpRoute route) {
        WayangA2uiHttpRequest resolvedRequest = Objects.requireNonNull(request, "request");
        WayangA2uiHttpRoute resolvedRoute = Objects.requireNonNull(route, "route");
        if (!resolvedRoute.method(resolvedRequest)) {
            return Optional.of(methodNotAllowed(resolvedRequest, resolvedRoute));
        }
        return validateResponseNegotiation(resolvedRequest, resolvedRoute)
                .or(() -> validateRequestContentType(resolvedRequest, resolvedRoute));
    }

    public Optional<WayangA2uiHttpResponse> validateOptions(
            WayangA2uiHttpRequest request,
            WayangA2uiHttpRoute route) {
        return validateResponseNegotiation(
                Objects.requireNonNull(request, "request"),
                Objects.requireNonNull(route, "route"));
    }

    public WayangA2uiHttpResponse routeResponse(WayangA2uiHttpRoute route, WayangA2uiHttpResponse response) {
        return Objects.requireNonNull(response, "response")
                .withRoute(Objects.requireNonNull(route, "route"));
    }

    private Optional<WayangA2uiHttpResponse> validateResponseNegotiation(
            WayangA2uiHttpRequest request,
            WayangA2uiHttpRoute route) {
        if (request.accepts(route.responseContentType())) {
            return Optional.empty();
        }
        return Optional.of(notAcceptable(request, route));
    }

    private Optional<WayangA2uiHttpResponse> validateRequestContentType(
            WayangA2uiHttpRequest request,
            WayangA2uiHttpRoute route) {
        if (!route.requestBodyRequired() || request.contentType(route.requestContentType())) {
            return Optional.empty();
        }
        return Optional.of(unsupportedMediaType(request, route));
    }

    private WayangA2uiHttpResponse methodNotAllowed(WayangA2uiHttpRequest request, WayangA2uiHttpRoute route) {
        return routeResponse(route, WayangA2uiHttpResponse.error(
                405,
                "method_not_allowed",
                "A2UI HTTP route " + request.path() + " only supports " + route.method() + "."));
    }

    private WayangA2uiHttpResponse unsupportedMediaType(WayangA2uiHttpRequest request, WayangA2uiHttpRoute route) {
        String actual = request.contentType().isBlank() ? "none" : request.contentType();
        return routeResponse(route, WayangA2uiHttpResponse.error(
                415,
                "unsupported_media_type",
                "A2UI HTTP route " + request.path() + " requires Content-Type "
                        + route.requestContentType() + ", received " + actual + "."));
    }

    private WayangA2uiHttpResponse notAcceptable(WayangA2uiHttpRequest request, WayangA2uiHttpRoute route) {
        String actual = request.accept().isBlank() ? "none" : request.accept();
        return routeResponse(route, WayangA2uiHttpResponse.error(
                406,
                "not_acceptable",
                "A2UI HTTP route " + request.path() + " produces " + route.responseContentType()
                        + ", but Accept was " + actual + "."));
    }
}

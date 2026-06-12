package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aHttpRoute;

import java.util.List;
import java.util.Objects;

/**
 * Applies route-aware HTTP response decoration and fallback responses.
 */
final class WayangA2aHttpRouteResponsePolicy {

    private final WayangA2aHttpRouteMatcher matcher;
    private final WayangA2aHttpRouteGuard guard;

    private WayangA2aHttpRouteResponsePolicy(
            WayangA2aHttpRouteMatcher matcher,
            WayangA2aHttpRouteGuard guard) {
        this.matcher = Objects.requireNonNull(matcher, "matcher");
        this.guard = guard == null ? WayangA2aHttpRouteGuard.strict() : guard;
    }

    static WayangA2aHttpRouteResponsePolicy from(
            WayangA2aHttpRouteMatcher matcher,
            WayangA2aHttpRouteGuard guard) {
        return new WayangA2aHttpRouteResponsePolicy(matcher, guard);
    }

    WayangA2aHttpResponse options(WayangA2aHttpRequest request) {
        WayangA2aHttpRequest resolvedRequest = Objects.requireNonNull(request, "request");
        return matcher.routeForPath(resolvedRequest.path())
                .map(route -> guard.validateOptions(resolvedRequest, route)
                        .map(response -> withAllow(response, route))
                        .orElseGet(() -> WayangA2aHttpRouteResponses.options(route, guard)))
                .orElseGet(() -> notFound(resolvedRequest));
    }

    WayangA2aHttpResponse unmatched(WayangA2aHttpRequest request) {
        WayangA2aHttpRequest resolvedRequest = Objects.requireNonNull(request, "request");
        List<A2aHttpRoute> pathRoutes = matcher.routesForPath(resolvedRequest.path());
        if (!pathRoutes.isEmpty()) {
            return methodNotAllowed(resolvedRequest, pathRoutes);
        }
        return notFound(resolvedRequest);
    }

    WayangA2aHttpResponse routeResponse(A2aHttpRoute route, WayangA2aHttpResponse response) {
        return guard.routeResponse(route, response);
    }

    WayangA2aHttpResponse withAllow(WayangA2aHttpResponse response, A2aHttpRoute route) {
        return WayangA2aHttpRouteResponses.withAllow(response, route);
    }

    private WayangA2aHttpResponse methodNotAllowed(
            WayangA2aHttpRequest request,
            List<A2aHttpRoute> pathRoutes) {
        A2aHttpRoute route = pathRoutes.getFirst();
        return WayangA2aHttpRouteResponses.withAllow(
                routeResponse(route, WayangA2aHttpResponse.error(
                        405,
                        "method_not_allowed",
                        "A2A HTTP route " + request.path() + " does not support " + request.method() + ".")),
                pathRoutes);
    }

    private static WayangA2aHttpResponse notFound(WayangA2aHttpRequest request) {
        return WayangA2aHttpResponse.error(
                404,
                "route_not_found",
                "No A2A HTTP route matches " + request.path() + ".");
    }
}

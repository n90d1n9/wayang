package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aHttpRoute;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds HTTP route metadata responses shared by dependency-free dispatchers.
 */
final class WayangA2aHttpRouteResponses {

    private WayangA2aHttpRouteResponses() {
    }

    static WayangA2aHttpResponse options(A2aHttpRoute route, WayangA2aHttpRouteGuard guard) {
        A2aHttpRoute resolvedRoute = Objects.requireNonNull(route, "route");
        WayangA2aHttpRouteGuard resolvedGuard = Objects.requireNonNull(guard, "guard");
        return withAllow(
                resolvedGuard.routeResponse(
                        resolvedRoute,
                        WayangA2aHttpResponse.object(200, optionsPayload(resolvedRoute))),
                resolvedRoute);
    }

    static WayangA2aHttpResponse withAllow(WayangA2aHttpResponse response, A2aHttpRoute route) {
        return withAllow(response, List.of(Objects.requireNonNull(route, "route")));
    }

    static WayangA2aHttpResponse withAllow(WayangA2aHttpResponse response, List<A2aHttpRoute> routes) {
        return Objects.requireNonNull(response, "response")
                .withHeaders(allowHeaders(routes));
    }

    static String allowHeader(A2aHttpRoute route) {
        return allowHeader(List.of(Objects.requireNonNull(route, "route")));
    }

    static String allowHeader(List<A2aHttpRoute> routes) {
        List<String> methods = Objects.requireNonNull(routes, "routes").stream()
                .filter(Objects::nonNull)
                .map(A2aHttpRoute::httpMethod)
                .distinct()
                .toList();
        return methods.isEmpty() ? "OPTIONS" : String.join(", ", methods) + ", OPTIONS";
    }

    static Map<String, Object> optionsPayload(A2aHttpRoute route) {
        A2aHttpRoute resolved = Objects.requireNonNull(route, "route");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", resolved.operation());
        payload.put("httpMethod", resolved.httpMethod());
        payload.put("path", resolved.path());
        payload.put("streaming", resolved.streaming());
        return WayangA2aMaps.copyMap(payload);
    }

    private static Map<String, Object> allowHeaders(List<A2aHttpRoute> routes) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(WayangA2aHttpResponse.HEADER_ALLOW, allowHeader(routes));
        return WayangA2aMaps.copyMap(headers);
    }
}

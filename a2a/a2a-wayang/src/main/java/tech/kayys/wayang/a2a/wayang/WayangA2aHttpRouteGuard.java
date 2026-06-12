package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.Objects;
import java.util.Optional;

/**
 * Reusable HTTP route guard for dependency-free A2A bindings.
 */
public final class WayangA2aHttpRouteGuard {

    private static final WayangA2aHttpRouteGuard STRICT = new WayangA2aHttpRouteGuard();

    public static WayangA2aHttpRouteGuard strict() {
        return STRICT;
    }

    public Optional<WayangA2aHttpResponse> validate(WayangA2aHttpRequest request, A2aHttpRoute route) {
        WayangA2aHttpRequest resolvedRequest = Objects.requireNonNull(request, "request");
        A2aHttpRoute resolvedRoute = Objects.requireNonNull(route, "route");
        return validateResponseNegotiation(resolvedRequest, resolvedRoute)
                .or(() -> validateRequestContentType(resolvedRequest, resolvedRoute))
                .or(() -> validateRequestBody(resolvedRequest, resolvedRoute));
    }

    public Optional<WayangA2aHttpResponse> validateOptions(WayangA2aHttpRequest request, A2aHttpRoute route) {
        return validateResponseNegotiation(
                Objects.requireNonNull(request, "request"),
                Objects.requireNonNull(route, "route"));
    }

    public WayangA2aHttpResponse routeResponse(A2aHttpRoute route, WayangA2aHttpResponse response) {
        return Objects.requireNonNull(response, "response")
                .withRoute(Objects.requireNonNull(route, "route"));
    }

    public String responseContentType(A2aHttpRoute route) {
        return route != null && route.streaming()
                ? A2aProtocol.EVENT_STREAM_MEDIA_TYPE
                : A2aProtocol.MEDIA_TYPE;
    }

    private Optional<WayangA2aHttpResponse> validateResponseNegotiation(
            WayangA2aHttpRequest request,
            A2aHttpRoute route) {
        if (request.accepts(responseContentType(route))) {
            return Optional.empty();
        }
        String actual = request.accept().isBlank() ? "none" : request.accept();
        return Optional.of(routeResponse(route, WayangA2aHttpResponse.error(
                406,
                "not_acceptable",
                "A2A HTTP route " + request.path() + " produces " + responseContentType(route)
                        + ", but Accept was " + actual + ".")));
    }

    private Optional<WayangA2aHttpResponse> validateRequestContentType(
            WayangA2aHttpRequest request,
            A2aHttpRoute route) {
        if (request.body().isBlank() || request.jsonContentType()) {
            return Optional.empty();
        }
        String actual = request.contentType().isBlank() ? "none" : request.contentType();
        return Optional.of(routeResponse(route, WayangA2aHttpResponse.error(
                415,
                "unsupported_media_type",
                "A2A HTTP route " + request.path() + " requires Content-Type application/json or "
                        + A2aProtocol.MEDIA_TYPE + ", received " + actual + ".")));
    }

    private Optional<WayangA2aHttpResponse> validateRequestBody(
            WayangA2aHttpRequest request,
            A2aHttpRoute route) {
        if (!requestBodyRequired(route) || !request.body().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(routeResponse(route, WayangA2aHttpResponse.error(
                400,
                "missing_request_body",
                "A2A HTTP route " + request.path() + " requires a request body.")));
    }

    private static boolean requestBodyRequired(A2aHttpRoute route) {
        return switch (route.operation()) {
            case A2aProtocol.OPERATION_SEND_MESSAGE,
                 A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE,
                 A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG -> true;
            default -> false;
        };
    }
}

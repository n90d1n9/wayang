package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpRouteProjection;

import java.util.List;
import java.util.Map;

/**
 * Framework-neutral descriptor for one A2UI HTTP binding.
 */
public record WayangA2uiHttpRoute(
        String operation,
        String method,
        String path,
        String requestContentType,
        String responseContentType,
        boolean requestBodyRequired) {

    public static final String OPERATION_EXCHANGE = "a2ui.exchange";
    public static final String OPERATION_SURFACE_CATALOG = "a2ui.surfaceCatalog";
    public static final String OPERATION_ROUTE_CATALOG = "a2ui.routeCatalog";
    public static final String OPERATION_BINDING_REPORT = "a2ui.bindingReport";
    public static final String OPERATION_SMOKE = "a2ui.smoke";
    public static final String OPERATION_READINESS = "a2ui.readiness";

    public static final String PATH_ROOT = "/a2ui";
    public static final String PATH_EXCHANGE = PATH_ROOT + "/exchange";
    public static final String PATH_SURFACE_CATALOG = PATH_ROOT + "/surface-catalog";
    public static final String PATH_ROUTE_CATALOG = PATH_ROOT + "/route-catalog";
    public static final String PATH_BINDING_REPORT = PATH_ROOT + "/binding-report";
    public static final String PATH_SMOKE = PATH_ROOT + "/smoke";
    public static final String PATH_READINESS = PATH_ROOT + "/readiness";

    public WayangA2uiHttpRoute {
        operation = normalizeOperation(operation);
        method = WayangA2uiHttpRequest.normalizeMethod(method);
        path = WayangA2uiHttpRequest.normalizePath(path);
        requestContentType = normalizeContentType(requestContentType);
        responseContentType = normalizeContentType(responseContentType);
        if (responseContentType.isBlank()) {
            responseContentType = WayangA2uiTransportContent.MIME_JSON;
        }
    }

    public static WayangA2uiHttpRoute exchange() {
        return new WayangA2uiHttpRoute(
                OPERATION_EXCHANGE,
                "POST",
                PATH_EXCHANGE,
                WayangA2uiTransportContent.MIME_JSON,
                WayangA2uiTransportContent.MIME_JSON,
                true);
    }

    public static WayangA2uiHttpRoute surfaceCatalog() {
        return new WayangA2uiHttpRoute(
                OPERATION_SURFACE_CATALOG,
                "GET",
                PATH_SURFACE_CATALOG,
                "",
                WayangA2uiTransportContent.MIME_JSON,
                false);
    }

    public static WayangA2uiHttpRoute routeCatalog() {
        return new WayangA2uiHttpRoute(
                OPERATION_ROUTE_CATALOG,
                "GET",
                PATH_ROUTE_CATALOG,
                "",
                WayangA2uiTransportContent.MIME_JSON,
                false);
    }

    public static WayangA2uiHttpRoute bindingReport() {
        return new WayangA2uiHttpRoute(
                OPERATION_BINDING_REPORT,
                "GET",
                PATH_BINDING_REPORT,
                "",
                WayangA2uiTransportContent.MIME_JSON,
                false);
    }

    public static WayangA2uiHttpRoute smoke() {
        return new WayangA2uiHttpRoute(
                OPERATION_SMOKE,
                "GET",
                PATH_SMOKE,
                "",
                WayangA2uiTransportContent.MIME_JSON,
                false);
    }

    public static WayangA2uiHttpRoute readiness() {
        return new WayangA2uiHttpRoute(
                OPERATION_READINESS,
                "GET",
                PATH_READINESS,
                "",
                WayangA2uiTransportContent.MIME_JSON,
                false);
    }

    public boolean matches(WayangA2uiHttpRequest request) {
        return method(request) && path(request);
    }

    public boolean operation(String expectedOperation) {
        return operation.equals(normalizeOperation(expectedOperation));
    }

    public boolean method(WayangA2uiHttpRequest request) {
        return request != null && request.method(method);
    }

    public boolean path(WayangA2uiHttpRequest request) {
        return request != null && request.path(path);
    }

    public List<String> allowedMethods() {
        return List.of(method, "OPTIONS");
    }

    public WayangA2uiHttpRoute withPath(String path) {
        return new WayangA2uiHttpRoute(
                operation,
                method,
                path,
                requestContentType,
                responseContentType,
                requestBodyRequired);
    }

    public String allowHeader() {
        return String.join(", ", allowedMethods());
    }

    public Map<String, Object> toMap() {
        return HttpRouteProjection.route(this);
    }

    private static String normalizeOperation(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("A2UI HTTP route operation must not be blank");
        }
        return normalized;
    }

    private static String normalizeContentType(String value) {
        return value == null ? "" : value.trim();
    }
}

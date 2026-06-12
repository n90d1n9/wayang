package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

record WayangA2aJsonRpcHttpRouteDescriptor(
        String key,
        String routeName,
        String operation,
        boolean enabled,
        String path,
        String httpMethod,
        String allow,
        String requestMediaType,
        List<String> responseMediaTypes,
        boolean requestBodyRequired) {

    static final String KEY_ENDPOINT = "endpoint";
    static final String KEY_SMOKE = "smoke";
    static final String KEY_ROUTE_CATALOG = "routeCatalog";
    static final String KEY_DIAGNOSTICS_REPORT = "diagnosticsReport";
    static final String KEY_SPEC_COMPLIANCE_REPORT = "specComplianceReport";
    static final String KEY_BINDING_REPORT = "bindingReport";
    static final String KEY_READINESS = "readiness";
    static final String KEY_READINESS_ISSUE_SUMMARY = "readinessIssueSummary";

    WayangA2aJsonRpcHttpRouteDescriptor {
        key = WayangA2aMaps.required(key, "key");
        routeName = WayangA2aMaps.required(routeName, "routeName");
        operation = WayangA2aMaps.required(operation, "operation");
        path = WayangA2aHttpRequest.normalizePath(path);
        httpMethod = WayangA2aHttpRequest.normalizeMethod(httpMethod);
        allow = allow == null || allow.isBlank()
                ? String.join(", ", httpMethod, "OPTIONS")
                : allow.trim();
        requestMediaType = requestMediaType == null ? "" : requestMediaType.trim();
        responseMediaTypes = WayangA2aMediaTypes.copyDistinct(responseMediaTypes);
        if (responseMediaTypes.isEmpty()) {
            responseMediaTypes = List.of(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        }
    }

    static List<WayangA2aJsonRpcHttpRouteDescriptor> fromConfig(WayangA2aJsonRpcHttpConfig config) {
        WayangA2aJsonRpcHttpConfig resolved = Objects.requireNonNull(config, "config");
        return WayangA2aJsonRpcHttpRouteSurface.ordered().stream()
                .map(surface -> surface.descriptor(resolved))
                .toList();
    }

    WayangA2aJsonRpcHttpRoute toRoute() {
        return new WayangA2aJsonRpcHttpRoute(
                operation,
                enabled,
                path,
                httpMethod,
                requestMediaType,
                responseMediaTypes,
                requestBodyRequired);
    }

    boolean matchesPath(WayangA2aHttpRequest request) {
        return enabled && request != null && request.path().equals(path);
    }

    WayangA2aHttpResponse optionsResponse() {
        return WayangA2aJsonRpcHttpRouteProjection.optionsResponse(this);
    }

    Map<String, Object> toBindingReportMap() {
        return WayangA2aJsonRpcHttpRouteProjection.bindingReportRoute(this);
    }
}

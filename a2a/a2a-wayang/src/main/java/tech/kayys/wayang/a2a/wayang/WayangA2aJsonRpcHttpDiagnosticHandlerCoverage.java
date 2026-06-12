package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

record WayangA2aJsonRpcHttpDiagnosticHandlerCoverage(
        boolean reported,
        List<String> routeKeys,
        List<String> handlerKeys,
        List<String> missingHandlerKeys,
        List<String> orphanHandlerKeys) {

    WayangA2aJsonRpcHttpDiagnosticHandlerCoverage {
        routeKeys = normalize(routeKeys);
        handlerKeys = normalize(handlerKeys);
        missingHandlerKeys = normalize(missingHandlerKeys);
        orphanHandlerKeys = normalize(orphanHandlerKeys);
    }

    static WayangA2aJsonRpcHttpDiagnosticHandlerCoverage from(
            List<WayangA2aJsonRpcHttpRouteDescriptor> routes,
            List<String> handlerKeys) {
        List<String> routeKeys = diagnosticRouteKeys(routes);
        List<String> resolvedHandlerKeys = normalize(handlerKeys);
        return new WayangA2aJsonRpcHttpDiagnosticHandlerCoverage(
                true,
                routeKeys,
                resolvedHandlerKeys,
                routeKeys.stream()
                        .filter(key -> !resolvedHandlerKeys.contains(key))
                        .toList(),
                resolvedHandlerKeys.stream()
                        .filter(key -> !routeKeys.contains(key))
                        .toList());
    }

    static WayangA2aJsonRpcHttpDiagnosticHandlerCoverage fromMap(Map<?, ?> values) {
        return WayangA2aJsonRpcHttpDiagnosticHandlerCoverageProjection.fromMap(values);
    }

    int routeKeyCount() {
        return routeKeys.size();
    }

    int handlerKeyCount() {
        return handlerKeys.size();
    }

    boolean complete() {
        return reported && missingHandlerKeys.isEmpty() && orphanHandlerKeys.isEmpty();
    }

    Map<String, Object> toMap() {
        return WayangA2aJsonRpcHttpDiagnosticHandlerCoverageProjection.coverage(this);
    }

    private static List<String> diagnosticRouteKeys(List<WayangA2aJsonRpcHttpRouteDescriptor> routes) {
        if (routes == null || routes.isEmpty()) {
            return List.of();
        }
        return routes.stream()
                .filter(Objects::nonNull)
                .filter(route -> !WayangA2aJsonRpcHttpRouteDescriptor.KEY_ENDPOINT.equals(route.key()))
                .map(WayangA2aJsonRpcHttpRouteDescriptor::key)
                .distinct()
                .toList();
    }

    private static List<String> normalize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

}

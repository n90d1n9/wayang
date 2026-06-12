package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WayangA2aSpecAlignmentRouteRequirements {

    private WayangA2aSpecAlignmentRouteRequirements() {
    }

    static List<WayangA2aSpecAlignmentRequirement> from(A2aHttpRouteCatalog routeCatalog) {
        A2aHttpRouteCatalog resolvedCatalog = routeCatalog == null
                ? A2aHttpRouteCatalog.standard()
                : routeCatalog;
        return A2aHttpRouteCatalog.standard().routes().stream()
                .map(route -> requirementFor(resolvedCatalog, route))
                .toList();
    }

    static WayangA2aSpecAlignmentRequirement requirementFor(
            A2aHttpRouteCatalog catalog,
            A2aHttpRoute expectedRoute) {
        return catalog.routeForOperation(expectedRoute.operation())
                .map(actualRoute -> routeRequirement(expectedRoute, actualRoute))
                .orElseGet(() -> WayangA2aSpecAlignmentRequirementFactory.from(
                        routeRequirementId(expectedRoute.operation()),
                        "route",
                        "A2A route " + expectedRoute.operation(),
                        false,
                        routeShape(expectedRoute),
                        Map.of("present", false),
                        "A2A route is missing from the local route catalog."));
    }

    static Map<String, Object> routeShape(A2aHttpRoute route) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("present", true);
        values.put("operation", route.operation());
        values.put("jsonRpcMethod", route.jsonRpcMethod());
        values.put("grpcMethod", route.grpcMethod());
        values.put("httpMethod", route.httpMethod());
        values.put("path", route.path());
        values.put("streaming", route.streaming());
        return WayangA2aMaps.copyMap(values);
    }

    static String routeRequirementId(String operation) {
        return "route." + operation;
    }

    private static WayangA2aSpecAlignmentRequirement routeRequirement(
            A2aHttpRoute expectedRoute,
            A2aHttpRoute actualRoute) {
        Map<String, Object> expected = routeShape(expectedRoute);
        Map<String, Object> actual = routeShape(actualRoute);
        return WayangA2aSpecAlignmentRequirementFactory.compare(
                routeRequirementId(expectedRoute.operation()),
                "route",
                "A2A route " + expectedRoute.operation(),
                expected,
                actual,
                "A2A route shape does not match the pinned v1.0 snapshot.");
    }
}

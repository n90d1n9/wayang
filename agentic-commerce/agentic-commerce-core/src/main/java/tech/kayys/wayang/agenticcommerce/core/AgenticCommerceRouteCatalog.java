package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manifest of Agentic Commerce Protocol HTTP routes.
 */
public record AgenticCommerceRouteCatalog(List<AgenticCommerceHttpRoute> routes) {

    public AgenticCommerceRouteCatalog {
        routes = routes == null
                ? List.of()
                : routes.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
    }

    public static AgenticCommerceRouteCatalog checkoutCatalog() {
        return new AgenticCommerceRouteCatalog(List.of(
                AgenticCommerceHttpRoute.createCheckoutSession(),
                AgenticCommerceHttpRoute.retrieveCheckoutSession(),
                AgenticCommerceHttpRoute.updateCheckoutSession(),
                AgenticCommerceHttpRoute.completeCheckoutSession(),
                AgenticCommerceHttpRoute.cancelCheckoutSession()));
    }

    public int routeCount() {
        return routes.size();
    }

    public Optional<AgenticCommerceHttpRoute> route(AgenticCommerceHttpRequest request) {
        return routes.stream()
                .filter(route -> route.matches(request))
                .findFirst();
    }

    public Optional<AgenticCommerceHttpRoute> routeForPath(String path) {
        AgenticCommerceHttpRequest request = AgenticCommerceHttpRequest.get(path);
        return routes.stream()
                .filter(route -> route.path(request))
                .findFirst();
    }

    public Optional<AgenticCommerceHttpRoute> routeForOperation(String operation) {
        return routes.stream()
                .filter(route -> route.operation(operation))
                .findFirst();
    }

    public AgenticCommerceSpecAlignmentReport specAlignmentReport() {
        return AgenticCommerceSpecAlignmentReport.from(this);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", "agentic-commerce");
        values.put("specVersion", AgenticCommerceProtocol.SPEC_VERSION);
        values.put("routeCount", routeCount());
        values.put("routes", routes.stream()
                .map(AgenticCommerceHttpRoute::toMap)
                .toList());
        return AgenticCommerceMaps.copy(values);
    }
}

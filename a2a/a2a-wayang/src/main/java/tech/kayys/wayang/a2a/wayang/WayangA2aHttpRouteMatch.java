package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aHttpRoute;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Matched A2A route plus extracted path parameters.
 */
public record WayangA2aHttpRouteMatch(A2aHttpRoute route, Map<String, String> pathParameters) {

    public WayangA2aHttpRouteMatch {
        if (route == null) {
            throw new IllegalArgumentException("route must not be null");
        }
        pathParameters = pathParameters == null || pathParameters.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(pathParameters));
    }

    public String operation() {
        return route.operation();
    }

    public Optional<String> pathParameter(String name) {
        return Optional.ofNullable(pathParameters.get(name));
    }
}

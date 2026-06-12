package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Matches normalized HTTP requests against A2A route templates.
 */
public final class WayangA2aHttpRouteMatcher {

    private final A2aHttpRouteCatalog catalog;

    public WayangA2aHttpRouteMatcher(A2aHttpRouteCatalog catalog) {
        this.catalog = catalog == null ? A2aHttpRouteCatalog.standard() : catalog;
    }

    public Optional<WayangA2aHttpRouteMatch> match(WayangA2aHttpRequest request) {
        WayangA2aHttpRequest resolved = Objects.requireNonNull(request, "request");
        return catalog.routes().stream()
                .filter(route -> route.httpMethod().equals(resolved.method()))
                .map(route -> matchPath(route, resolved.path()))
                .flatMap(Optional::stream)
                .findFirst();
    }

    public Optional<A2aHttpRoute> routeForPath(String path) {
        String normalizedPath = WayangA2aHttpRequest.normalizePath(path);
        return catalog.routes().stream()
                .filter(route -> matchPath(route, normalizedPath).isPresent())
                .findFirst();
    }

    public List<A2aHttpRoute> routesForPath(String path) {
        String normalizedPath = WayangA2aHttpRequest.normalizePath(path);
        return catalog.routes().stream()
                .filter(route -> matchPath(route, normalizedPath).isPresent())
                .toList();
    }

    private static Optional<WayangA2aHttpRouteMatch> matchPath(A2aHttpRoute route, String path) {
        String[] templateSegments = segments(route.path());
        String[] pathSegments = segments(path);
        if (templateSegments.length != pathSegments.length) {
            return Optional.empty();
        }
        Map<String, String> parameters = new LinkedHashMap<>();
        for (int i = 0; i < templateSegments.length; i++) {
            if (!matchSegment(templateSegments[i], pathSegments[i], parameters)) {
                return Optional.empty();
            }
        }
        return Optional.of(new WayangA2aHttpRouteMatch(route, parameters));
    }

    private static boolean matchSegment(String template, String value, Map<String, String> parameters) {
        int start = template.indexOf('{');
        int end = template.indexOf('}', start + 1);
        if (start < 0 || end < 0) {
            return template.equals(value);
        }
        String prefix = template.substring(0, start);
        String suffix = template.substring(end + 1);
        String name = template.substring(start + 1, end).trim();
        if (name.isBlank() || !value.startsWith(prefix) || !value.endsWith(suffix)) {
            return false;
        }
        String captured = value.substring(prefix.length(), value.length() - suffix.length());
        if (captured.isBlank()) {
            return false;
        }
        parameters.put(name, URLDecoder.decode(captured, StandardCharsets.UTF_8));
        return true;
    }

    private static String[] segments(String path) {
        String normalized = WayangA2aHttpRequest.normalizePath(path);
        if ("/".equals(normalized)) {
            return new String[0];
        }
        return normalized.substring(1).split("/");
    }
}

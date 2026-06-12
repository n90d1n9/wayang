package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpRouteProjection;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manifest of A2UI HTTP routes for concrete web framework bindings.
 */
public record WayangA2uiHttpRouteCatalog(List<WayangA2uiHttpRoute> routes) {

    public WayangA2uiHttpRouteCatalog {
        routes = routes == null
                ? List.of()
                : routes.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
    }

    public static WayangA2uiHttpRouteCatalog defaultCatalog() {
        return new WayangA2uiHttpRouteCatalog(List.of(
                WayangA2uiHttpRoute.exchange(),
                WayangA2uiHttpRoute.surfaceCatalog(),
                WayangA2uiHttpRoute.routeCatalog(),
                WayangA2uiHttpRoute.bindingReport(),
                WayangA2uiHttpRoute.smoke(),
                WayangA2uiHttpRoute.readiness()));
    }

    public WayangA2uiHttpRouteCatalog mountedAt(String rootPath) {
        String root = normalizeRootPath(rootPath);
        if (WayangA2uiHttpRoute.PATH_ROOT.equals(root)) {
            return this;
        }
        return new WayangA2uiHttpRouteCatalog(routes.stream()
                .map(route -> route.withPath(mountPath(root, route.path())))
                .toList());
    }

    public int routeCount() {
        return routes.size();
    }

    public Optional<WayangA2uiHttpRoute> route(String method, String path) {
        WayangA2uiHttpRequest request = new WayangA2uiHttpRequest(method, path, "", Map.of(), Map.of());
        return routes.stream()
                .filter(route -> route.matches(request))
                .findFirst();
    }

    public Optional<WayangA2uiHttpRoute> routeForPath(String path) {
        WayangA2uiHttpRequest request = WayangA2uiHttpRequest.get(path);
        return routes.stream()
                .filter(route -> route.path(request))
                .findFirst();
    }

    public Optional<WayangA2uiHttpRoute> routeForOperation(String operation) {
        return routes.stream()
                .filter(route -> route.operation(operation))
                .findFirst();
    }

    public WayangA2uiSpecAlignmentReport specAlignmentReport() {
        return WayangA2uiSpecAlignmentReport.from(this);
    }

    public Map<String, Object> toMap() {
        return HttpRouteProjection.catalog(this);
    }

    private static String normalizeRootPath(String rootPath) {
        if (rootPath == null || rootPath.isBlank()) {
            return WayangA2uiHttpRoute.PATH_ROOT;
        }
        String normalized = WayangA2uiHttpRequest.normalizePath(rootPath);
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String mountPath(String rootPath, String routePath) {
        String normalized = WayangA2uiHttpRequest.normalizePath(routePath);
        String suffix = normalized;
        if (normalized.equals(WayangA2uiHttpRoute.PATH_ROOT)) {
            suffix = "";
        } else if (normalized.startsWith(WayangA2uiHttpRoute.PATH_ROOT + "/")) {
            suffix = normalized.substring(WayangA2uiHttpRoute.PATH_ROOT.length());
        }
        if ("/".equals(rootPath)) {
            return suffix.isBlank() ? "/" : suffix;
        }
        return rootPath + suffix;
    }
}

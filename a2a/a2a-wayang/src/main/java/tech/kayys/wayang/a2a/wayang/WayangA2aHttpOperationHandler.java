package tech.kayys.wayang.a2a.wayang;

/**
 * Handler SPI for concrete A2A HTTP operations.
 */
@FunctionalInterface
public interface WayangA2aHttpOperationHandler {

    WayangA2aHttpResponse handle(WayangA2aHttpRequest request, WayangA2aHttpRouteMatch routeMatch);
}

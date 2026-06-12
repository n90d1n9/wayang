package tech.kayys.wayang.a2a.wayang;

import java.util.Objects;

/**
 * Shared header projection helpers for HTTP-shaped A2A responses.
 */
final class WayangA2aHttpResponseHeaders {

    private WayangA2aHttpResponseHeaders() {
    }

    static String header(WayangA2aHttpResponse response, String name) {
        Object value = Objects.requireNonNull(response, "response").headers().get(name);
        return value == null ? "" : String.valueOf(value);
    }

    static String routeOperationHeader(WayangA2aHttpResponse response) {
        return header(response, WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION);
    }

    static String allowHeader(WayangA2aHttpResponse response) {
        return header(response, WayangA2aHttpResponse.HEADER_ALLOW);
    }

    static String protocolVersionHeader(WayangA2aHttpResponse response) {
        String protocolVersion = header(response, WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION);
        return protocolVersion.isBlank()
                ? header(response, WayangA2aHttpResponse.HEADER_A2A_VERSION)
                : protocolVersion;
    }
}

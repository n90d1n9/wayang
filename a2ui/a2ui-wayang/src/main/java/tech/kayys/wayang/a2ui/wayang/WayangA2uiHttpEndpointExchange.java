package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;
import java.util.Objects;

/**
 * Framework-friendly projection of one raw inbound A2UI HTTP exchange.
 */
public record WayangA2uiHttpEndpointExchange(
        WayangA2uiHttpEndpointRequest request,
        WayangA2uiHttpEndpointResponse response) {

    public WayangA2uiHttpEndpointExchange {
        request = Objects.requireNonNull(request, "request");
        response = Objects.requireNonNull(response, "response");
    }

    public static WayangA2uiHttpEndpointExchange from(
            WayangA2uiHttpEndpointBinding endpoint,
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers) {
        return from(endpoint, method, rawPath, body, headers, Map.of());
    }

    public static WayangA2uiHttpEndpointExchange from(
            WayangA2uiHttpEndpointBinding endpoint,
            String method,
            String rawPath,
            String body,
            Map<?, ?> headers,
            Map<?, ?> attributes) {
        WayangA2uiHttpEndpointBinding resolved = Objects.requireNonNull(endpoint, "endpoint");
        return new WayangA2uiHttpEndpointExchange(
                resolved.project(method, rawPath, body, headers, attributes),
                resolved.respond(method, rawPath, body, headers, attributes));
    }

    public int statusCode() {
        return response.statusCode();
    }

    public boolean successful() {
        return response.successful();
    }

    public boolean knownPath() {
        return request.knownPath();
    }

    public boolean matched() {
        return request.matched();
    }

    public WayangA2uiTransportResponse transportResponse() {
        return WayangA2uiTransportResponse.fromJson(response.body());
    }

    public WayangA2uiTransportOutcome outcome() {
        return transportResponse().outcome();
    }

    public boolean transportError() {
        return transportResponse().transportError().isPresent();
    }

    public Map<String, Object> responseEnvelope() {
        return transportResponse().toMap();
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpEndpointProjection.exchange(this);
    }
}

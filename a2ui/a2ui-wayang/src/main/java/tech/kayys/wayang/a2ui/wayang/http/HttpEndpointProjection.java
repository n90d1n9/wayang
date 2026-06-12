package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointExchange;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointRequest;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpEndpointResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportResponse;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projection helpers for A2UI HTTP endpoint request/response views.
 */
public final class HttpEndpointProjection {

    private HttpEndpointProjection() {
    }

    public static Map<String, Object> request(WayangA2uiHttpEndpointRequest request) {
        WayangA2uiHttpEndpointRequest resolved = Objects.requireNonNull(request, "request");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("method", resolved.method());
        values.put("path", resolved.path());
        values.put("body", resolved.body());
        values.put("headers", resolved.headers());
        values.put("attributes", resolved.attributes());
        values.put("knownPath", resolved.knownPath());
        values.put("matched", resolved.matched());
        resolved.operation().ifPresent(operation -> values.put("operation", operation));
        resolved.allow().ifPresent(allow -> values.put("allow", allow));
        values.put("requestBodyRequired", resolved.requestBodyRequired());
        values.put("route", resolved.route());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> response(WayangA2uiHttpEndpointResponse response) {
        WayangA2uiHttpEndpointResponse resolved = Objects.requireNonNull(response, "response");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", resolved.statusCode());
        values.put("contentType", resolved.contentType());
        values.put("body", resolved.body());
        values.put("headerCount", resolved.headers().size());
        values.put("headers", resolved.headers());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> exchange(WayangA2uiHttpEndpointExchange exchange) {
        WayangA2uiHttpEndpointExchange resolved = Objects.requireNonNull(exchange, "exchange");
        WayangA2uiTransportResponse transport = resolved.transportResponse();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("knownPath", resolved.knownPath());
        values.put("matched", resolved.matched());
        values.put("statusCode", resolved.statusCode());
        values.put("successful", resolved.successful());
        values.put("outcome", transport.outcome().name());
        values.put("transportError", transport.transportError().isPresent());
        values.put("request", resolved.request().toMap());
        values.put("response", resolved.response().toMap());
        values.put("responseEnvelope", transport.toMap());
        return TransportMaps.freeze(values);
    }
}

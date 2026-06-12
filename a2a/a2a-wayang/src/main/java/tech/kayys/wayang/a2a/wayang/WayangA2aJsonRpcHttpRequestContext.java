package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

record WayangA2aJsonRpcHttpRequestContext(Object jsonRpcId, String method) {

    WayangA2aJsonRpcHttpRequestContext {
        method = WayangA2aMaps.optional(method);
    }

    static WayangA2aJsonRpcHttpRequestContext from(WayangA2aHttpRequest request) {
        WayangA2aHttpRequest resolved = Objects.requireNonNull(request, "request");
        if (resolved.body().isBlank()) {
            return empty();
        }
        try {
            Map<String, Object> values = WayangA2aHttpJson.read(resolved.body());
            Object id = values.get("id");
            return new WayangA2aJsonRpcHttpRequestContext(id, method(values).orElse(null));
        } catch (IllegalArgumentException ignored) {
            return empty();
        }
    }

    static WayangA2aJsonRpcHttpRequestContext empty() {
        return new WayangA2aJsonRpcHttpRequestContext(null, null);
    }

    Optional<Object> id() {
        return Optional.ofNullable(jsonRpcId);
    }

    Optional<String> methodName() {
        return Optional.ofNullable(method);
    }

    String methodOr(String fallback) {
        return methodName().orElseGet(() -> WayangA2aMaps.required(fallback, "fallback"));
    }

    String responseMediaType() {
        return methodName()
                .filter(WayangA2aJsonRpcMethods::streaming)
                .map(ignored -> A2aProtocol.EVENT_STREAM_MEDIA_TYPE)
                .orElse(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
    }

    private static Optional<String> method(Map<String, Object> values) {
        try {
            return Optional.of(WayangA2aJsonRpcRequest.fromMap(values).method());
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}

package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Result for one A2A JSON-RPC scenario exchange.
 */
public record WayangA2aJsonRpcScenarioExchangeResult(
        int index,
        WayangA2aJsonRpcScenarioExchange exchange,
        WayangA2aHttpResponse response,
        Map<String, Object> decodedBody,
        List<Map<String, Object>> decodedEvents) {

    public WayangA2aJsonRpcScenarioExchangeResult {
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
        if (exchange == null) {
            throw new IllegalArgumentException("exchange must not be null");
        }
        if (response == null) {
            throw new IllegalArgumentException("response must not be null");
        }
        decodedBody = WayangA2aMaps.copyMap(decodedBody);
        decodedEvents = decodedEvents == null || decodedEvents.isEmpty()
                ? List.of()
                : decodedEvents.stream().map(WayangA2aMaps::copyMap).toList();
    }

    public boolean successful() {
        return response.successful() && error().isEmpty();
    }

    public Optional<Object> requestId() {
        return Optional.ofNullable(exchange.request().id());
    }

    public String method() {
        return exchange.request().method();
    }

    public Optional<Map<String, Object>> error() {
        Object error = decodedBody.get("error");
        if (error instanceof Map<?, ?> map) {
            return Optional.of(WayangA2aMaps.copyMap(map));
        }
        for (Map<String, Object> event : decodedEvents) {
            Object eventError = event.get("error");
            if (eventError instanceof Map<?, ?> map) {
                return Optional.of(WayangA2aMaps.copyMap(map));
            }
        }
        return Optional.empty();
    }

    public Map<String, Object> toMap() {
        return WayangA2aScenarioExchangeResultProjection.exchange(
                index,
                requestId().orElse(null),
                method(),
                null,
                response.statusCode(),
                successful(),
                response.contentType(),
                null,
                error().orElse(null),
                response.headers(),
                body());
    }

    private Object body() {
        if (!decodedEvents.isEmpty()) {
            return decodedEvents;
        }
        if (!decodedBody.isEmpty()) {
            return decodedBody;
        }
        return response.body();
    }
}

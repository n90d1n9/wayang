package tech.kayys.wayang.a2a.wayang;

import java.util.Map;
import java.util.Optional;

/**
 * Result for one A2A HTTP scenario exchange.
 */
public record WayangA2aHttpScenarioExchangeResult(
        int index,
        WayangA2aHttpScenarioExchange exchange,
        WayangA2aHttpResponse response,
        Map<String, Object> decodedBody) {

    public WayangA2aHttpScenarioExchangeResult {
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
    }

    public boolean successful() {
        return response.successful();
    }

    public Optional<String> operation() {
        Object operation = response.headers().get(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION);
        return Optional.ofNullable(WayangA2aMaps.optional(operation));
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> error() {
        Object error = decodedBody.get("error");
        if (error instanceof Map<?, ?> map) {
            return Optional.of((Map<String, Object>) WayangA2aMaps.copyMap(map));
        }
        return Optional.empty();
    }

    public Map<String, Object> toMap() {
        return WayangA2aScenarioExchangeResultProjection.exchange(
                index,
                null,
                exchange.request().method(),
                exchange.request().path(),
                response.statusCode(),
                successful(),
                response.contentType(),
                operation().orElse(null),
                error().orElse(null),
                response.headers(),
                decodedBody.isEmpty() ? response.body() : decodedBody);
    }
}

package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs dependency-free A2A JSON-RPC scenarios through a dispatcher.
 */
public final class WayangA2aJsonRpcHarness {

    private final WayangA2aJsonRpcDispatcher dispatcher;

    public WayangA2aJsonRpcHarness(WayangA2aJsonRpcDispatcher dispatcher) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        this.dispatcher = dispatcher;
    }

    public static WayangA2aJsonRpcHarness of(WayangA2aJsonRpcDispatcher dispatcher) {
        return new WayangA2aJsonRpcHarness(dispatcher);
    }

    public WayangA2aJsonRpcScenarioResult run(WayangA2aJsonRpcScenario scenario) {
        List<WayangA2aJsonRpcScenarioExchangeResult> results = new ArrayList<>();
        List<WayangA2aJsonRpcScenarioIssue> issues = new ArrayList<>();
        int index = 0;
        for (WayangA2aJsonRpcScenarioExchange exchange : scenario.exchanges()) {
            WayangA2aJsonRpcScenarioExchangeResult result = runExchange(index, exchange);
            results.add(result);
            if (!result.successful()) {
                issues.add(WayangA2aJsonRpcScenarioIssue.from(scenario.id(), result));
            }
            index++;
        }
        return new WayangA2aJsonRpcScenarioResult(scenario, results, issues);
    }

    private WayangA2aJsonRpcScenarioExchangeResult runExchange(
            int index,
            WayangA2aJsonRpcScenarioExchange exchange) {
        WayangA2aHttpResponse response = dispatcher.dispatchJson(exchange.request().toJson());
        return new WayangA2aJsonRpcScenarioExchangeResult(
                index,
                exchange,
                response,
                decodeBody(response),
                decodeEvents(response));
    }

    private static Map<String, Object> decodeBody(WayangA2aHttpResponse response) {
        if (response.body().isBlank() || eventStream(response)) {
            return Map.of();
        }
        try {
            return WayangA2aHttpJson.read(response.body());
        } catch (IllegalArgumentException ignored) {
            return Map.of();
        }
    }

    private static List<Map<String, Object>> decodeEvents(WayangA2aHttpResponse response) {
        if (!eventStream(response) || response.body().isBlank()) {
            return List.of();
        }
        List<Map<String, Object>> events = new ArrayList<>();
        for (String line : response.body().split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("data:")) {
                continue;
            }
            String json = trimmed.substring("data:".length()).trim();
            if (json.isBlank()) {
                continue;
            }
            try {
                events.add(WayangA2aHttpJson.read(json));
            } catch (IllegalArgumentException ignored) {
                // Non-JSON SSE data lines are ignored by the smoke harness.
            }
        }
        return List.copyOf(events);
    }

    private static boolean eventStream(WayangA2aHttpResponse response) {
        return response.contentType().startsWith(A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
    }
}

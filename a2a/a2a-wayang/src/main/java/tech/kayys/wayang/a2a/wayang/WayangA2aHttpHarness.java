package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs dependency-free A2A HTTP scenarios through a dispatcher.
 */
public final class WayangA2aHttpHarness {

    private final WayangA2aHttpOperationDispatcher dispatcher;

    public WayangA2aHttpHarness(WayangA2aHttpOperationDispatcher dispatcher) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        this.dispatcher = dispatcher;
    }

    public static WayangA2aHttpHarness of(WayangA2aHttpOperationDispatcher dispatcher) {
        return new WayangA2aHttpHarness(dispatcher);
    }

    public WayangA2aHttpScenarioResult run(WayangA2aHttpScenario scenario) {
        List<WayangA2aHttpScenarioExchangeResult> results = new ArrayList<>();
        List<WayangA2aHttpScenarioIssue> issues = new ArrayList<>();
        int index = 0;
        for (WayangA2aHttpScenarioExchange exchange : scenario.exchanges()) {
            WayangA2aHttpScenarioExchangeResult result = runExchange(index, exchange);
            results.add(result);
            if (!result.successful()) {
                issues.add(WayangA2aHttpScenarioIssue.from(scenario.id(), result));
            }
            index++;
        }
        return new WayangA2aHttpScenarioResult(scenario, results, issues);
    }

    private WayangA2aHttpScenarioExchangeResult runExchange(int index, WayangA2aHttpScenarioExchange exchange) {
        WayangA2aHttpResponse response = dispatcher.dispatch(exchange.request());
        Map<String, Object> decodedBody = decodeBody(response);
        return new WayangA2aHttpScenarioExchangeResult(index, exchange, response, decodedBody);
    }

    private static Map<String, Object> decodeBody(WayangA2aHttpResponse response) {
        if (response.body().isBlank() || response.contentType().startsWith("text/event-stream")) {
            return Map.of();
        }
        try {
            return WayangA2aHttpJson.read(response.body());
        } catch (IllegalArgumentException ignored) {
            return Map.of();
        }
    }
}

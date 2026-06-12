package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;

/**
 * Aggregate result for one A2A JSON-RPC scenario run.
 */
public record WayangA2aJsonRpcScenarioResult(
        WayangA2aJsonRpcScenario scenario,
        List<WayangA2aJsonRpcScenarioExchangeResult> exchanges,
        List<WayangA2aJsonRpcScenarioIssue> issues) {

    public WayangA2aJsonRpcScenarioResult {
        if (scenario == null) {
            throw new IllegalArgumentException("scenario must not be null");
        }
        exchanges = exchanges == null ? List.of() : List.copyOf(exchanges);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean passed() {
        return issues.isEmpty() && exchanges.size() == scenario.exchanges().size();
    }

    public int exchangeCount() {
        return exchanges.size();
    }

    public Map<String, Object> toMap() {
        return WayangA2aScenarioResultProjection.result(
                scenario.id(),
                passed(),
                exchangeCount(),
                exchanges,
                WayangA2aJsonRpcScenarioExchangeResult::toMap,
                issues,
                WayangA2aJsonRpcScenarioIssue::toMap,
                scenario.attributes());
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }
}

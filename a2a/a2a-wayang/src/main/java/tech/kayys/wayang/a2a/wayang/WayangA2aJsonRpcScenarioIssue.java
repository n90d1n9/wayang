package tech.kayys.wayang.a2a.wayang;

import java.util.Map;

/**
 * Compact failure summary for one A2A JSON-RPC scenario exchange.
 */
public record WayangA2aJsonRpcScenarioIssue(
        String scenarioId,
        int exchangeIndex,
        Object requestId,
        String method,
        int statusCode,
        String code,
        String message) {

    public WayangA2aJsonRpcScenarioIssue {
        scenarioId = WayangA2aMaps.required(scenarioId, "scenarioId");
        method = WayangA2aMaps.required(method, "method");
        code = WayangA2aScenarioIssueProjection.code(
                code,
                WayangA2aScenarioIssueProjection.jsonRpcFallbackCode(statusCode));
        message = WayangA2aScenarioIssueProjection.message(message, "A2A JSON-RPC exchange failed");
    }

    public static WayangA2aJsonRpcScenarioIssue from(
            String scenarioId,
            WayangA2aJsonRpcScenarioExchangeResult result) {
        Map<String, Object> error = result.error().orElse(Map.of());
        return new WayangA2aJsonRpcScenarioIssue(
                scenarioId,
                result.index(),
                result.requestId().orElse(null),
                result.method(),
                result.response().statusCode(),
                WayangA2aMaps.optional(error.get("code")),
                WayangA2aMaps.optional(error.get("message")));
    }

    public Map<String, Object> toMap() {
        return WayangA2aScenarioIssueProjection.issue(
                scenarioId,
                exchangeIndex,
                requestId,
                method,
                null,
                statusCode,
                null,
                code,
                message);
    }
}

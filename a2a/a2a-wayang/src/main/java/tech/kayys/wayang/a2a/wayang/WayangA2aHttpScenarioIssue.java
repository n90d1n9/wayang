package tech.kayys.wayang.a2a.wayang;

import java.util.Map;

/**
 * Compact failure summary for one A2A HTTP scenario exchange.
 */
public record WayangA2aHttpScenarioIssue(
        String scenarioId,
        int exchangeIndex,
        String method,
        String path,
        int statusCode,
        String operation,
        String code,
        String message) {

    public WayangA2aHttpScenarioIssue {
        scenarioId = WayangA2aMaps.required(scenarioId, "scenarioId");
        method = WayangA2aMaps.required(method, "method");
        path = WayangA2aMaps.required(path, "path");
        operation = WayangA2aMaps.optional(operation);
        code = WayangA2aScenarioIssueProjection.code(
                code,
                WayangA2aScenarioIssueProjection.httpFallbackCode(statusCode));
        message = WayangA2aScenarioIssueProjection.message(message, "A2A HTTP exchange failed");
    }

    public static WayangA2aHttpScenarioIssue from(String scenarioId, WayangA2aHttpScenarioExchangeResult result) {
        Map<String, Object> error = result.error().orElse(Map.of());
        return new WayangA2aHttpScenarioIssue(
                scenarioId,
                result.index(),
                result.exchange().request().method(),
                result.exchange().request().path(),
                result.response().statusCode(),
                result.operation().orElse(null),
                WayangA2aMaps.optional(error.get("code")),
                WayangA2aMaps.optional(error.get("message")));
    }

    public Map<String, Object> toMap() {
        return WayangA2aScenarioIssueProjection.issue(
                scenarioId,
                exchangeIndex,
                null,
                method,
                path,
                statusCode,
                operation,
                code,
                message);
    }
}

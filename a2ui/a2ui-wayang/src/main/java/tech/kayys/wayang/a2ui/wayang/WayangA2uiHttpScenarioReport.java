package tech.kayys.wayang.a2ui.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Stable machine-readable projection of an A2UI HTTP scenario result.
 */
public record WayangA2uiHttpScenarioReport(
        String scenarioId,
        int exchangeCount,
        long successfulCount,
        long clientErrorCount,
        long serverErrorCount,
        long handledCount,
        long rejectedCount,
        boolean transportErrors,
        List<Integer> statusCodes,
        List<String> outcomes,
        List<Map<String, Object>> exchanges,
        List<Map<String, Object>> issues,
        Map<String, Object> attributes) {

    public WayangA2uiHttpScenarioReport {
        scenarioId = scenarioId == null || scenarioId.isBlank() ? "a2ui-http-scenario" : scenarioId.trim();
        exchangeCount = Math.max(0, exchangeCount);
        successfulCount = Math.max(0L, successfulCount);
        clientErrorCount = Math.max(0L, clientErrorCount);
        serverErrorCount = Math.max(0L, serverErrorCount);
        handledCount = Math.max(0L, handledCount);
        rejectedCount = Math.max(0L, rejectedCount);
        statusCodes = statusCodes == null ? List.of() : List.copyOf(statusCodes);
        outcomes = outcomes == null
                ? List.of()
                : outcomes.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .toList();
        exchanges = WayangA2uiTransportMaps.copyMaps(exchanges);
        issues = WayangA2uiTransportMaps.copyMaps(issues);
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpScenarioReport from(WayangA2uiHttpScenarioResult result) {
        WayangA2uiHttpScenarioResult resolved = Objects.requireNonNull(result, "result");
        return new WayangA2uiHttpScenarioReport(
                resolved.scenarioId(),
                resolved.exchangeCount(),
                resolved.successfulCount(),
                resolved.clientErrorCount(),
                resolved.serverErrorCount(),
                resolved.handledCount(),
                resolved.rejectedCount(),
                resolved.hasTransportErrors(),
                resolved.statusCodes(),
                resolved.outcomes().stream()
                        .map(WayangA2uiTransportOutcome::name)
                        .toList(),
                resolved.exchanges().stream()
                        .map(WayangA2uiHttpScenarioProjection::exchange)
                        .toList(),
                resolved.exchanges().stream()
                        .map(exchange -> WayangA2uiHttpScenarioIssue.from(resolved.scenarioId(), exchange))
                        .flatMap(Optional::stream)
                        .map(WayangA2uiHttpScenarioIssue::toMap)
                        .toList(),
                resolved.attributes());
    }

    public int issueCount() {
        return issues.size();
    }

    public boolean passed() {
        return issueCount() == 0 && clientErrorCount == 0L && serverErrorCount == 0L && !transportErrors;
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpScenarioProjection.report(this);
    }

    public String toJson() {
        return WayangA2uiTransportJson.json(toMap(), "Unable to encode A2UI HTTP scenario report");
    }
}

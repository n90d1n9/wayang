package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.http.HttpScenarioProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

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
        scenarioId = RecordValues.textOrDefault(scenarioId, "a2ui-http-scenario");
        exchangeCount = RecordNumbers.nonNegative(exchangeCount);
        successfulCount = RecordNumbers.nonNegative(successfulCount);
        clientErrorCount = RecordNumbers.nonNegative(clientErrorCount);
        serverErrorCount = RecordNumbers.nonNegative(serverErrorCount);
        handledCount = RecordNumbers.nonNegative(handledCount);
        rejectedCount = RecordNumbers.nonNegative(rejectedCount);
        statusCodes = RecordCollections.copyList(statusCodes);
        outcomes = DecodeCollections.nonBlankTexts(outcomes);
        exchanges = TransportMaps.copyMaps(exchanges);
        issues = TransportMaps.copyMaps(issues);
        attributes = TransportMaps.copy(attributes);
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
                        .map(HttpScenarioProjection::exchange)
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
        return HttpScenarioProjection.report(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI HTTP scenario report");
    }
}

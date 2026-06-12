package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Validation result for a checkout HTTP harness expectation.
 */
public record AgenticCommerceCheckoutHttpExpectationResult(
        AgenticCommerceCheckoutHttpExpectation expectation,
        AgenticCommerceCheckoutHttpScenarioResult scenarioResult,
        List<AgenticCommerceValidationIssue> issues,
        Map<String, Object> metadata) {

    public AgenticCommerceCheckoutHttpExpectationResult {
        expectation = Objects.requireNonNull(expectation, "expectation");
        scenarioResult = Objects.requireNonNull(scenarioResult, "scenarioResult");
        issues = issues == null
                ? List.of()
                : issues.stream()
                        .filter(Objects::nonNull)
                        .toList();
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public boolean valid() {
        return issues.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("expectationId", expectation.id());
        values.put("scenarioId", scenarioResult.scenario().id());
        values.put("valid", valid());
        values.put("issueCount", issueCount());
        values.put("expectedValid", expectation.expectedValid());
        values.put("actualValid", scenarioResult.valid());
        values.put("expectedSuccessful", expectation.expectedSuccessful());
        values.put("actualSuccessful", scenarioResult.successful());
        values.put("expectedExchangeCount", expectation.expectedExchangeCount());
        values.put("actualExchangeCount", scenarioResult.exchangeCount());
        values.put("expectedIssueCount", expectation.expectedIssueCount());
        values.put("actualIssueCount", scenarioResult.issueCount());
        values.put("expectation", expectation.toMap());
        values.put("scenarioSummary", scenarioSummary());
        AgenticCommerceValues.putList(values, "issues", issues.stream()
                .map(AgenticCommerceValidationIssue::toMap)
                .toList());
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }

    private Map<String, Object> scenarioSummary() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("valid", scenarioResult.valid());
        values.put("successful", scenarioResult.successful());
        values.put("stepCount", scenarioResult.scenario().stepCount());
        values.put("exchangeCount", scenarioResult.exchangeCount());
        values.put("issueCount", scenarioResult.issueCount());
        values.put("statusCodes", scenarioResult.exchanges().stream()
                .map(exchange -> exchange.result().response().statusCode())
                .toList());
        values.put("operations", scenarioResult.exchanges().stream()
                .map(AgenticCommerceCheckoutHttpExchange::operation)
                .toList());
        values.put("stepIds", scenarioResult.exchanges().stream()
                .map(exchange -> exchange.step().id())
                .toList());
        values.put("transportErrorCount", scenarioResult.exchanges().stream()
                .filter(exchange -> !exchange.transportError().isBlank())
                .count());
        return AgenticCommerceMaps.copy(values);
    }
}

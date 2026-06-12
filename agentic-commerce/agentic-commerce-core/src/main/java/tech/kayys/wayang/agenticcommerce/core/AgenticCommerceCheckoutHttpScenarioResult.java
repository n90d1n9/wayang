package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregate checkout HTTP harness scenario result.
 */
public record AgenticCommerceCheckoutHttpScenarioResult(
        AgenticCommerceCheckoutHttpScenario scenario,
        List<AgenticCommerceCheckoutHttpExchange> exchanges,
        List<AgenticCommerceValidationIssue> issues,
        Map<String, Object> metadata) {

    public AgenticCommerceCheckoutHttpScenarioResult {
        scenario = Objects.requireNonNull(scenario, "scenario");
        exchanges = exchanges == null
                ? List.of()
                : exchanges.stream()
                        .filter(Objects::nonNull)
                        .toList();
        issues = issues == null
                ? List.of()
                : issues.stream()
                        .filter(Objects::nonNull)
                        .toList();
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public boolean valid() {
        return issues.isEmpty() && exchanges.stream().allMatch(AgenticCommerceCheckoutHttpExchange::valid);
    }

    public boolean successful() {
        return valid() && exchanges.size() == scenario.stepCount()
                && exchanges.stream().allMatch(AgenticCommerceCheckoutHttpExchange::successful);
    }

    public int exchangeCount() {
        return exchanges.size();
    }

    public int issueCount() {
        return issues.size() + exchanges.stream()
                .mapToInt(AgenticCommerceCheckoutHttpExchange::issueCount)
                .sum();
    }

    public AgenticCommerceCheckoutHttpExpectationResult validate(
            AgenticCommerceCheckoutHttpExpectation expectation) {
        AgenticCommerceCheckoutHttpExpectation resolved = expectation == null
                ? AgenticCommerceCheckoutHttpExpectation.smoke()
                : expectation;
        return resolved.validate(this);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("scenarioId", scenario.id());
        values.put("valid", valid());
        values.put("successful", successful());
        values.put("stepCount", scenario.stepCount());
        values.put("exchangeCount", exchangeCount());
        values.put("issueCount", issueCount());
        values.put("scenario", scenario.toMap());
        values.put("exchanges", exchanges.stream()
                .map(AgenticCommerceCheckoutHttpExchange::toMap)
                .toList());
        AgenticCommerceValues.putList(values, "issues", issues.stream()
                .map(AgenticCommerceValidationIssue::toMap)
                .toList());
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}

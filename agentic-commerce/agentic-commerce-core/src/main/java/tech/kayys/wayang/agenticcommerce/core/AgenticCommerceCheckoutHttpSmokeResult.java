package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Operational checkout HTTP smoke result with exit-code metadata.
 */
public record AgenticCommerceCheckoutHttpSmokeResult(
        AgenticCommerceCheckoutHttpScenarioResult scenarioResult,
        AgenticCommerceCheckoutHttpExpectationResult expectationResult,
        int exitCode,
        Map<String, Object> metadata) {

    public AgenticCommerceCheckoutHttpSmokeResult {
        scenarioResult = Objects.requireNonNull(scenarioResult, "scenarioResult");
        expectationResult = Objects.requireNonNull(expectationResult, "expectationResult");
        exitCode = exitCode < 0 ? 1 : exitCode;
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceCheckoutHttpSmokeResult of(
            AgenticCommerceCheckoutHttpScenarioResult scenarioResult,
            AgenticCommerceCheckoutHttpExpectationResult expectationResult,
            Map<?, ?> metadata) {
        boolean passed = expectationResult != null && expectationResult.valid();
        return new AgenticCommerceCheckoutHttpSmokeResult(
                scenarioResult,
                expectationResult,
                passed ? 0 : 1,
                AgenticCommerceMaps.copy(metadata));
    }

    public boolean passed() {
        return expectationResult.valid();
    }

    public boolean failed() {
        return !passed();
    }

    public boolean successfulExit() {
        return exitCode == 0;
    }

    public int scenarioIssueCount() {
        return scenarioResult.issueCount();
    }

    public int expectationIssueCount() {
        return expectationResult.issueCount();
    }

    public int issueCount() {
        return scenarioIssueCount() + expectationIssueCount();
    }

    public AgenticCommerceCheckoutHttpSmokeSummary summary() {
        return AgenticCommerceCheckoutHttpSmokeSummary.from(this);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", passed());
        values.put("failed", failed());
        values.put("exitCode", exitCode);
        values.put("successfulExit", successfulExit());
        values.put("scenarioId", scenarioResult.scenario().id());
        values.put("expectationId", expectationResult.expectation().id());
        values.put("scenarioValid", scenarioResult.valid());
        values.put("scenarioSuccessful", scenarioResult.successful());
        values.put("expectationValid", expectationResult.valid());
        values.put("exchangeCount", scenarioResult.exchangeCount());
        values.put("scenarioIssueCount", scenarioIssueCount());
        values.put("expectationIssueCount", expectationIssueCount());
        values.put("issueCount", issueCount());
        values.put("scenarioResult", scenarioResult.toMap());
        values.put("expectationResult", expectationResult.toMap());
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}

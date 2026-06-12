package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpExpectationResult;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpScenarioResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * JSON-ready report for an Agentic Commerce connector contract run.
 */
public record AgenticCommerceConnectorContractReport(
        String connectorName,
        AgenticCommerceWayangRuntimeConfig runtimeConfig,
        AgenticCommerceCheckoutHttpScenarioResult scenarioResult,
        AgenticCommerceCheckoutHttpExpectationResult expectationResult,
        Map<String, Object> attributes) {

    public AgenticCommerceConnectorContractReport {
        connectorName = AgenticCommerceWayangMaps.text(connectorName);
        runtimeConfig = runtimeConfig == null ? AgenticCommerceWayangRuntimeConfig.defaults() : runtimeConfig;
        scenarioResult = Objects.requireNonNull(scenarioResult, "scenarioResult");
        expectationResult = Objects.requireNonNull(expectationResult, "expectationResult");
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public boolean passed() {
        return scenarioResult.successful() && expectationResult.valid();
    }

    public int exchangeCount() {
        return scenarioResult.exchangeCount();
    }

    public int issueCount() {
        return scenarioResult.issueCount() + expectationResult.issueCount();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", passed());
        values.put("connectorName", connectorName);
        values.put("exchangeCount", exchangeCount());
        values.put("issueCount", issueCount());
        values.put("scenarioId", scenarioResult.scenario().id());
        values.put("expectationId", expectationResult.expectation().id());
        values.put("runtimeConfig", runtimeConfig.toMap());
        values.put("scenarioResult", scenarioResult.toMap());
        values.put("expectationResult", expectationResult.toMap());
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }
}

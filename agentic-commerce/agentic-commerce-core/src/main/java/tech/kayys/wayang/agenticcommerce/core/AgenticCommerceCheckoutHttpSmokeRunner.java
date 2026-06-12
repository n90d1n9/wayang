package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Operational runner for Agentic Commerce checkout HTTP smoke probes.
 */
public final class AgenticCommerceCheckoutHttpSmokeRunner {

    private final AgenticCommerceCheckoutHttpHarness harness;

    public AgenticCommerceCheckoutHttpSmokeRunner() {
        this(AgenticCommerceCheckoutHttpHarness.checkout());
    }

    public AgenticCommerceCheckoutHttpSmokeRunner(AgenticCommerceCheckoutHttpHarness harness) {
        this.harness = harness == null ? AgenticCommerceCheckoutHttpHarness.checkout() : harness;
    }

    public static AgenticCommerceCheckoutHttpSmokeRunner checkout() {
        return new AgenticCommerceCheckoutHttpSmokeRunner();
    }

    public AgenticCommerceCheckoutHttpSmokeResult run() {
        return run(
                AgenticCommerceCheckoutHttpSmoke.scenario(),
                AgenticCommerceCheckoutHttpSmoke.responder(),
                AgenticCommerceCheckoutHttpExpectation.smoke());
    }

    public AgenticCommerceCheckoutHttpSmokeResult run(AgenticCommerceCheckoutHttpResponder responder) {
        return run(
                AgenticCommerceCheckoutHttpSmoke.scenario(),
                responder,
                AgenticCommerceCheckoutHttpExpectation.smoke());
    }

    public AgenticCommerceCheckoutHttpSmokeResult run(
            AgenticCommerceCheckoutHttpScenario scenario,
            AgenticCommerceCheckoutHttpResponder responder,
            AgenticCommerceCheckoutHttpExpectation expectation) {
        AgenticCommerceCheckoutHttpScenarioResult scenarioResult = harness.run(scenario, responder);
        AgenticCommerceCheckoutHttpExpectationResult expectationResult = scenarioResult.validate(expectation);
        return AgenticCommerceCheckoutHttpSmokeResult.of(
                scenarioResult,
                expectationResult,
                metadata(scenarioResult, expectationResult));
    }

    private static Map<String, Object> metadata(
            AgenticCommerceCheckoutHttpScenarioResult scenarioResult,
            AgenticCommerceCheckoutHttpExpectationResult expectationResult) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", "agentic-commerce");
        values.put("specVersion", AgenticCommerceProtocol.SPEC_VERSION);
        values.put("routeCount", AgenticCommerceRouteCatalog.checkoutCatalog().routeCount());
        values.put("scenarioId", scenarioResult.scenario().id());
        values.put("expectationId", expectationResult.expectation().id());
        values.put("exchangeCount", scenarioResult.exchangeCount());
        values.put("scenarioIssueCount", scenarioResult.issueCount());
        values.put("expectationIssueCount", expectationResult.issueCount());
        return AgenticCommerceMaps.copy(values);
    }
}

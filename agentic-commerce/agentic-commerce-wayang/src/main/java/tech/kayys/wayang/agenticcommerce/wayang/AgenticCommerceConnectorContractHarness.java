package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpExpectation;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpHarness;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpScenario;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpScenarioResult;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpSmoke;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Connector contract harness for the Agentic Commerce checkout lifecycle.
 */
public final class AgenticCommerceConnectorContractHarness {

    public static final String CONTRACT_CHECKOUT_LIFECYCLE = "agentic-commerce-checkout-lifecycle";

    private final AgenticCommerceCheckoutHttpHarness harness;
    private final AgenticCommerceCheckoutHttpScenario scenario;
    private final AgenticCommerceCheckoutHttpExpectation expectation;

    public AgenticCommerceConnectorContractHarness() {
        this(
                AgenticCommerceCheckoutHttpHarness.checkout(),
                AgenticCommerceCheckoutHttpSmoke.scenario(),
                AgenticCommerceCheckoutHttpExpectation.smoke());
    }

    public AgenticCommerceConnectorContractHarness(
            AgenticCommerceCheckoutHttpHarness harness,
            AgenticCommerceCheckoutHttpScenario scenario,
            AgenticCommerceCheckoutHttpExpectation expectation) {
        this.harness = harness == null ? AgenticCommerceCheckoutHttpHarness.checkout() : harness;
        this.scenario = scenario == null ? AgenticCommerceCheckoutHttpSmoke.scenario() : scenario;
        this.expectation = expectation == null ? AgenticCommerceCheckoutHttpExpectation.smoke() : expectation;
    }

    public static AgenticCommerceConnectorContractHarness checkoutLifecycle() {
        return new AgenticCommerceConnectorContractHarness();
    }

    public AgenticCommerceCheckoutHttpScenario scenario() {
        return scenario;
    }

    public AgenticCommerceCheckoutHttpExpectation expectation() {
        return expectation;
    }

    public AgenticCommerceConnectorContractReport run(AgenticCommerceConnector connector) {
        AgenticCommerceConnector resolved = Objects.requireNonNull(connector, "connector");
        return run(
                resolved,
                AgenticCommerceWayangRuntimeConfig.defaults(),
                resolved.getClass().getSimpleName());
    }

    public AgenticCommerceConnectorContractReport run(AgenticCommerceWayangRuntime runtime) {
        AgenticCommerceWayangRuntime resolved = Objects.requireNonNull(runtime, "runtime");
        return run(
                resolved.connector(),
                resolved.runtimeConfig(),
                resolved.connectorFactoryConfig().connectorKind());
    }

    public AgenticCommerceConnectorContractReport run(
            AgenticCommerceConnector connector,
            AgenticCommerceWayangRuntimeConfig runtimeConfig,
            String connectorName) {
        AgenticCommerceConnector resolved = Objects.requireNonNull(connector, "connector");
        AgenticCommerceWayangRuntimeConfig config = runtimeConfig == null
                ? AgenticCommerceWayangRuntimeConfig.defaults()
                : runtimeConfig;
        AgenticCommerceCheckoutHttpScenarioResult scenarioResult = harness.run(scenario, resolved);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("contractId", CONTRACT_CHECKOUT_LIFECYCLE);
        attributes.put("scenarioStepCount", scenario.stepCount());
        attributes.put("connectorFactoryKind", config.connectorFactoryConfig().connectorKind());
        return new AgenticCommerceConnectorContractReport(
                connectorName,
                config,
                scenarioResult,
                scenarioResult.validate(expectation),
                attributes);
    }
}

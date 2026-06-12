package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceCheckoutHttpExpectationTest {

    @Test
    void acceptsBuiltInCheckoutSmokeExpectation() {
        AgenticCommerceCheckoutHttpScenarioResult scenarioResult = AgenticCommerceCheckoutHttpSmoke.run();
        AgenticCommerceCheckoutHttpExpectationResult expectationResult = scenarioResult.validate(
                AgenticCommerceCheckoutHttpExpectation.smoke());

        assertThat(expectationResult.valid()).isTrue();
        assertThat(expectationResult.issueCount()).isZero();
        assertThat(expectationResult.toMap())
                .containsEntry("expectationId", "agentic-commerce-checkout-smoke-expectation")
                .containsEntry("scenarioId", "agentic-commerce-checkout-smoke")
                .containsEntry("valid", true)
                .containsEntry("actualExchangeCount", 5)
                .containsEntry("actualIssueCount", 0);
        assertThat(map(expectationResult.toMap().get("scenarioSummary")))
                .containsEntry("transportErrorCount", 0L);
    }

    @Test
    void defaultScenarioValidationUsesSmokeExpectation() {
        AgenticCommerceCheckoutHttpExpectationResult expectationResult = AgenticCommerceCheckoutHttpSmoke.run()
                .validate(null);

        assertThat(expectationResult.valid()).isTrue();
        assertThat(expectationResult.expectation().id()).isEqualTo("agentic-commerce-checkout-smoke-expectation");
    }

    @Test
    void reportsExpectationMismatches() {
        AgenticCommerceCheckoutHttpExpectation expectation = new AgenticCommerceCheckoutHttpExpectation(
                "wrong-shape",
                true,
                true,
                1,
                0,
                false,
                List.of(200),
                List.of(AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION),
                List.of("retrieve"),
                Map.of());

        AgenticCommerceCheckoutHttpExpectationResult result = AgenticCommerceCheckoutHttpSmoke.run()
                .validate(expectation);

        assertThat(result.valid()).isFalse();
        assertThat(result.issues())
                .extracting(AgenticCommerceValidationIssue::code)
                .containsExactly(
                        "unexpected_exchange_count",
                        "unexpected_status_codes",
                        "unexpected_operations",
                        "unexpected_step_ids");
        assertThat(result.toMap())
                .containsEntry("expectedExchangeCount", 1)
                .containsEntry("actualExchangeCount", 5);
    }

    @Test
    void reportsDisallowedTransportErrors() {
        AgenticCommerceCheckoutHttpScenarioResult scenarioResult = transportFailureResult();
        AgenticCommerceCheckoutHttpExpectation expectation = new AgenticCommerceCheckoutHttpExpectation(
                "transport-disallowed",
                false,
                false,
                1,
                scenarioResult.issueCount(),
                false,
                List.of(599),
                List.of(AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION),
                List.of("retrieve"),
                Map.of());

        AgenticCommerceCheckoutHttpExpectationResult result = scenarioResult.validate(expectation);

        assertThat(result.valid()).isFalse();
        assertThat(result.issues())
                .extracting(AgenticCommerceValidationIssue::code)
                .containsExactly("transport_errors_not_allowed");
    }

    @Test
    void allowsTransportErrorsWhenExpectationPermitsThem() {
        AgenticCommerceCheckoutHttpScenarioResult scenarioResult = transportFailureResult();
        AgenticCommerceCheckoutHttpExpectation expectation = new AgenticCommerceCheckoutHttpExpectation(
                "transport-allowed",
                false,
                false,
                1,
                scenarioResult.issueCount(),
                true,
                List.of(599),
                List.of(AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION),
                List.of("retrieve"),
                Map.of());

        AgenticCommerceCheckoutHttpExpectationResult result = scenarioResult.validate(expectation);

        assertThat(result.valid()).isTrue();
        assertThat(result.issueCount()).isZero();
    }

    private static AgenticCommerceCheckoutHttpScenarioResult transportFailureResult() {
        AgenticCommerceCheckoutHttpScenario scenario = new AgenticCommerceCheckoutHttpScenario(
                "transport-failure",
                "Transport failure",
                List.of(AgenticCommerceCheckoutHttpScenarioStep.successful(
                        "retrieve",
                        AgenticCommerceCheckoutHttpRequests.retrieve(
                                "cs_123",
                                AgenticCommerceHttpRequestOptions.bearer("token")),
                        200)),
                Map.of());
        return AgenticCommerceCheckoutHttpHarness.checkout()
                .run(scenario, request -> {
                    throw new IllegalStateException("seller offline");
                });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}

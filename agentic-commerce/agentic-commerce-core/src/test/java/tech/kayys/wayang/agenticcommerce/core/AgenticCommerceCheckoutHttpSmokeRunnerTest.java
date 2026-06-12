package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceCheckoutHttpSmokeRunnerTest {

    @Test
    void runsPassingCheckoutSmokeResult() {
        AgenticCommerceCheckoutHttpSmokeResult result = AgenticCommerceCheckoutHttpSmokeRunner.checkout().run();

        assertThat(result.passed()).isTrue();
        assertThat(result.failed()).isFalse();
        assertThat(result.exitCode()).isZero();
        assertThat(result.successfulExit()).isTrue();
        assertThat(result.scenarioIssueCount()).isZero();
        assertThat(result.expectationIssueCount()).isZero();
        assertThat(result.issueCount()).isZero();
        assertThat(result.scenarioResult().exchangeCount()).isEqualTo(5);
        assertThat(result.toMap())
                .containsEntry("passed", true)
                .containsEntry("exitCode", 0)
                .containsEntry("successfulExit", true)
                .containsEntry("scenarioId", "agentic-commerce-checkout-smoke")
                .containsEntry("expectationId", "agentic-commerce-checkout-smoke-expectation")
                .containsEntry("exchangeCount", 5);
        assertThat(map(result.toMap().get("metadata")))
                .containsEntry("protocol", "agentic-commerce")
                .containsEntry("routeCount", 5);
    }

    @Test
    void smokeFixtureConvenienceReturnsRunnerResult() {
        AgenticCommerceCheckoutHttpSmokeResult result = AgenticCommerceCheckoutHttpSmoke.result();

        assertThat(result.passed()).isTrue();
        assertThat(result.exitCode()).isZero();
    }

    @Test
    void failingDefaultSmokeExpectationProducesNonZeroExitCode() {
        AgenticCommerceCheckoutHttpSmokeResult result = AgenticCommerceCheckoutHttpSmokeRunner.checkout()
                .run(request -> AgenticCommerceHttpResponse
                        .json(500, AgenticCommerceJson.write(AgenticCommerceError.of("seller_error", "Seller failed.").toMap()))
                        .withHeaders(Map.of(AgenticCommerceProtocol.HEADER_REQUEST_ID, "req-fail")));

        assertThat(result.passed()).isFalse();
        assertThat(result.failed()).isTrue();
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.successfulExit()).isFalse();
        assertThat(result.issueCount()).isGreaterThan(0);
        assertThat(result.expectationResult().issues())
                .extracting(AgenticCommerceValidationIssue::code)
                .contains("unexpected_scenario_valid", "unexpected_scenario_successful", "unexpected_issue_count");
    }

    @Test
    void customExpectationCanAcceptFailureScenarios() {
        AgenticCommerceCheckoutHttpScenario scenario = new AgenticCommerceCheckoutHttpScenario(
                "accepted-failure",
                "Accepted failure",
                List.of(AgenticCommerceCheckoutHttpScenarioStep.successful(
                        "retrieve",
                        AgenticCommerceCheckoutHttpRequests.retrieve(
                                "cs_123",
                                AgenticCommerceHttpRequestOptions.bearer("token")),
                        200)),
                Map.of());
        AgenticCommerceCheckoutHttpScenarioResult scenarioResult = AgenticCommerceCheckoutHttpHarness.checkout()
                .run(scenario, request -> {
                    throw new IllegalStateException("seller offline");
                });
        AgenticCommerceCheckoutHttpExpectation expectation = new AgenticCommerceCheckoutHttpExpectation(
                "accepted-failure-expectation",
                false,
                false,
                1,
                scenarioResult.issueCount(),
                true,
                List.of(599),
                List.of(AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION),
                List.of("retrieve"),
                Map.of("accepted", true));

        AgenticCommerceCheckoutHttpSmokeResult result = AgenticCommerceCheckoutHttpSmokeRunner.checkout()
                .run(scenario, request -> {
                    throw new IllegalStateException("seller offline");
                }, expectation);

        assertThat(result.passed()).isTrue();
        assertThat(result.exitCode()).isZero();
        assertThat(result.scenarioResult().successful()).isFalse();
        assertThat(result.expectationResult().valid()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}

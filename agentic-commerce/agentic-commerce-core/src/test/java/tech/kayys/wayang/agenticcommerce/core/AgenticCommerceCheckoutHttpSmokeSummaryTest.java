package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceCheckoutHttpSmokeSummaryTest {

    @Test
    void summarizesPassingCheckoutSmokeResults() {
        AgenticCommerceCheckoutHttpSmokeSummary summary = AgenticCommerceCheckoutHttpSmoke.result().summary();

        assertThat(summary.passed()).isTrue();
        assertThat(summary.failed()).isFalse();
        assertThat(summary.exitCode()).isZero();
        assertThat(summary.successfulExit()).isTrue();
        assertThat(summary.scenarioId()).isEqualTo("agentic-commerce-checkout-smoke");
        assertThat(summary.expectationId()).isEqualTo("agentic-commerce-checkout-smoke-expectation");
        assertThat(summary.exchangeCount()).isEqualTo(5);
        assertThat(summary.issueCount()).isZero();
        assertThat(summary.routeCount()).isEqualTo(5);
        assertThat(summary.transportErrorCount()).isZero();
        assertThat(summary.statusCodes()).containsExactly(201, 200, 200, 200, 200);
        assertThat(summary.operations()).containsExactly(
                AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION,
                AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION,
                AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION,
                AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION,
                AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION);
        assertThat(summary.stepIds()).containsExactly("create", "retrieve", "update", "complete", "cancel");
        assertThat(summary.issues()).isEmpty();
        assertThat(summary.toMap())
                .containsEntry("passed", true)
                .containsEntry("routeCount", 5)
                .containsEntry("transportErrorCount", 0L);
    }

    @Test
    void decodesSmokeSummariesFromResultMapsAndJson() {
        AgenticCommerceCheckoutHttpSmokeResult smokeResult = AgenticCommerceCheckoutHttpSmoke.result();
        AgenticCommerceCheckoutHttpSmokeSummary fromMap = AgenticCommerceCheckoutHttpSmokeSummary.fromMap(smokeResult.toMap());
        AgenticCommerceCheckoutHttpSmokeSummary fromJson = AgenticCommerceCheckoutHttpSmokeSummary.fromJson(
                AgenticCommerceJson.write(smokeResult.toMap()));
        AgenticCommerceCheckoutHttpSmokeSummary fromCompactJson = AgenticCommerceCheckoutHttpSmokeSummary.fromJson(
                AgenticCommerceJson.write(smokeResult.summary().toMap()));

        assertThat(fromMap.passed()).isTrue();
        assertThat(fromMap.statusCodes()).containsExactly(201, 200, 200, 200, 200);
        assertThat(fromMap.operations()).containsExactlyElementsOf(smokeResult.summary().operations());
        assertThat(fromJson.passed()).isTrue();
        assertThat(fromJson.stepIds()).containsExactly("create", "retrieve", "update", "complete", "cancel");
        assertThat(fromJson.issueCount()).isZero();
        assertThat(fromCompactJson.passed()).isTrue();
        assertThat(fromCompactJson.statusCodes()).containsExactly(201, 200, 200, 200, 200);
        assertThat(fromCompactJson.operations()).containsExactlyElementsOf(smokeResult.summary().operations());
    }

    @Test
    void flattensFailureIssuesAcrossScenarioAndExpectationResults() {
        AgenticCommerceCheckoutHttpSmokeResult result = AgenticCommerceCheckoutHttpSmokeRunner.checkout()
                .run(request -> AgenticCommerceHttpResponse
                        .json(500, AgenticCommerceJson.write(AgenticCommerceError.of("seller_error", "Seller failed.").toMap()))
                        .withHeaders(Map.of(AgenticCommerceProtocol.HEADER_REQUEST_ID, "req-fail")));

        AgenticCommerceCheckoutHttpSmokeSummary summary = result.summary();

        assertThat(summary.passed()).isFalse();
        assertThat(summary.exitCode()).isEqualTo(1);
        assertThat(summary.issueCount()).isEqualTo(result.issueCount());
        assertThat(summary.transportErrorCount()).isZero();
        assertThat(summary.issues()).isNotEmpty();
        assertThat(summary.issues())
                .extracting(AgenticCommerceCheckoutHttpIssueSummary::source)
                .contains("exchange", "expectation");
        assertThat(summary.issues())
                .extracting(AgenticCommerceCheckoutHttpIssueSummary::code)
                .contains("unexpected_status", "unexpected_step_status", "unexpected_scenario_valid");
        assertThat(summary.toMap()).containsEntry("failed", true);
    }

    @Test
    void summarizesTransportFailuresEvenWhenExpectationAcceptsThem() {
        AgenticCommerceCheckoutHttpScenario scenario = new AgenticCommerceCheckoutHttpScenario(
                "accepted-transport-failure",
                "Accepted transport failure",
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
                "accepted-transport-failure-expectation",
                false,
                false,
                1,
                scenarioResult.issueCount(),
                true,
                List.of(599),
                List.of(AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION),
                List.of("retrieve"),
                Map.of());

        AgenticCommerceCheckoutHttpSmokeSummary summary = AgenticCommerceCheckoutHttpSmokeRunner.checkout()
                .run(scenario, request -> {
                    throw new IllegalStateException("seller offline");
                }, expectation)
                .summary();

        assertThat(summary.passed()).isTrue();
        assertThat(summary.scenarioSuccessful()).isFalse();
        assertThat(summary.expectationValid()).isTrue();
        assertThat(summary.exitCode()).isZero();
        assertThat(summary.transportErrorCount()).isEqualTo(1);
        assertThat(summary.issues())
                .extracting(AgenticCommerceCheckoutHttpIssueSummary::code)
                .contains("transport_error");
        assertThat(summary.issues())
                .extracting(AgenticCommerceCheckoutHttpIssueSummary::stepId)
                .contains("retrieve");
    }
}

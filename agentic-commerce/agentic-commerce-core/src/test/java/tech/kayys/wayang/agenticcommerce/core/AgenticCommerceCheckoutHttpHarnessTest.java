package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceCheckoutHttpHarnessTest {

    @Test
    void runsBuiltInCheckoutSmokeScenario() {
        AgenticCommerceCheckoutHttpScenarioResult result = AgenticCommerceCheckoutHttpSmoke.run();

        assertThat(result.valid()).isTrue();
        assertThat(result.successful()).isTrue();
        assertThat(result.exchangeCount()).isEqualTo(5);
        assertThat(result.issueCount()).isZero();
        assertThat(result.exchanges())
                .extracting(AgenticCommerceCheckoutHttpExchange::operation)
                .containsExactly(
                        AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION,
                        AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION,
                        AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION,
                        AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION,
                        AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION);
        assertThat(result.exchanges())
                .extracting(exchange -> exchange.result().checkoutSession().status())
                .containsExactly(
                        AgenticCommerceCheckoutStatus.OPEN,
                        AgenticCommerceCheckoutStatus.READY_FOR_PAYMENT,
                        AgenticCommerceCheckoutStatus.READY_FOR_PAYMENT,
                        AgenticCommerceCheckoutStatus.COMPLETED,
                        AgenticCommerceCheckoutStatus.CANCELED);
        assertThat(result.toMap())
                .containsEntry("scenarioId", "agentic-commerce-checkout-smoke")
                .containsEntry("successful", true)
                .containsEntry("exchangeCount", 5);
    }

    @Test
    void reportsTransportErrorsAsScenarioIssues() {
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

        AgenticCommerceCheckoutHttpScenarioResult result = AgenticCommerceCheckoutHttpHarness.checkout()
                .run(scenario, request -> {
                    throw new IllegalStateException("seller offline");
                });

        assertThat(result.valid()).isFalse();
        assertThat(result.successful()).isFalse();
        assertThat(result.exchangeCount()).isEqualTo(1);
        assertThat(result.issueCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.exchanges()).singleElement().satisfies(exchange -> {
            assertThat(exchange.transportError()).isEqualTo("seller offline");
            assertThat(exchange.result().response().statusCode()).isEqualTo(599);
            assertThat(exchange.issues())
                    .extracting(AgenticCommerceValidationIssue::code)
                    .contains("transport_error", "unexpected_step_status", "unexpected_step_success");
        });
    }

    @Test
    void reportsRequestValidationIssuesBeforeDispatch() {
        AgenticCommerceHttpRequest request = AgenticCommerceCheckoutHttpRequests.create(
                AgenticCommerceCreateCheckoutSessionRequest.fromMap(Map.of(
                        "buyer",
                        Map.of("email", "buyer@example.com"),
                        "items",
                        List.of(Map.of("id", "sku_1", "quantity", 1)))),
                AgenticCommerceHttpRequestOptions.defaults());
        AgenticCommerceCheckoutHttpScenario scenario = new AgenticCommerceCheckoutHttpScenario(
                "missing-auth",
                "Missing auth",
                List.of(AgenticCommerceCheckoutHttpScenarioStep.successful("create", request, 201)),
                Map.of());

        AgenticCommerceCheckoutHttpScenarioResult result = AgenticCommerceCheckoutHttpHarness.checkout()
                .run(scenario, AgenticCommerceCheckoutHttpSmoke.responder());

        assertThat(result.valid()).isFalse();
        assertThat(result.exchanges()).singleElement().satisfies(exchange -> {
            assertThat(exchange.requestValidation().valid()).isFalse();
            assertThat(exchange.issues())
                    .extracting(AgenticCommerceValidationIssue::code)
                    .contains("missing_authorization");
        });
    }

    @Test
    void exposesScenarioAndExchangeMapsForHarnessConsumers() {
        AgenticCommerceCheckoutHttpScenarioResult result = AgenticCommerceCheckoutHttpSmoke.run();
        Map<String, Object> values = result.toMap();

        assertThat(values).containsKeys("scenario", "exchanges", "metadata");
        assertThat(map(values.get("scenario")))
                .containsEntry("id", "agentic-commerce-checkout-smoke")
                .containsEntry("stepCount", 5);
        List<?> exchanges = (List<?>) values.get("exchanges");
        assertThat(exchanges).hasSize(5);
        assertThat(map(exchanges.get(0)))
                .containsEntry("stepId", "create")
                .containsEntry("operation", AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION)
                .containsEntry("actualStatusCode", 201)
                .containsEntry("successful", true);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}

package tech.kayys.wayang.agenticcommerce.core;

import java.util.List;
import java.util.Map;

/**
 * Built-in checkout smoke scenario and fixture responder.
 */
public final class AgenticCommerceCheckoutHttpSmoke {

    private static final String SESSION_ID = "cs_smoke";

    private AgenticCommerceCheckoutHttpSmoke() {
    }

    public static AgenticCommerceCheckoutHttpScenario scenario() {
        AgenticCommerceHttpRequestOptions options = AgenticCommerceHttpRequestOptions.bearer("smoke-token")
                .withIdempotencyKey("smoke-idem")
                .withRequestId("smoke-request");
        return new AgenticCommerceCheckoutHttpScenario(
                "agentic-commerce-checkout-smoke",
                "Agentic Commerce checkout smoke",
                List.of(
                        AgenticCommerceCheckoutHttpScenarioStep.successful(
                                "create",
                                AgenticCommerceCheckoutHttpRequests.create(createPayload(), options),
                                201),
                        AgenticCommerceCheckoutHttpScenarioStep.successful(
                                "retrieve",
                                AgenticCommerceCheckoutHttpRequests.retrieve(SESSION_ID, options),
                                200),
                        AgenticCommerceCheckoutHttpScenarioStep.successful(
                                "update",
                                AgenticCommerceCheckoutHttpRequests.update(SESSION_ID, updatePayload(), options),
                                200),
                        AgenticCommerceCheckoutHttpScenarioStep.successful(
                                "complete",
                                AgenticCommerceCheckoutHttpRequests.complete(SESSION_ID, completePayload(), options),
                                200),
                        AgenticCommerceCheckoutHttpScenarioStep.successful(
                                "cancel",
                                AgenticCommerceCheckoutHttpRequests.cancel(SESSION_ID, options),
                                200)),
                Map.of("fixture", true));
    }

    public static AgenticCommerceCheckoutHttpResponder responder() {
        return request -> {
            String operation = AgenticCommerceValues.text(request.attributes(), "operation");
            return switch (operation) {
                case AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION -> response(request, 201, AgenticCommerceCheckoutStatus.OPEN);
                case AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION -> response(request, 200, AgenticCommerceCheckoutStatus.READY_FOR_PAYMENT);
                case AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION -> response(request, 200, AgenticCommerceCheckoutStatus.READY_FOR_PAYMENT);
                case AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION -> response(request, 200, AgenticCommerceCheckoutStatus.COMPLETED);
                case AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION -> response(request, 200, AgenticCommerceCheckoutStatus.CANCELED);
                default -> AgenticCommerceHttpResponse
                        .json(404, AgenticCommerceJson.write(AgenticCommerceError.of("unknown_operation", operation).toMap()))
                        .withHeaders(responseHeaders(request));
            };
        };
    }

    public static AgenticCommerceCheckoutHttpScenarioResult run() {
        return AgenticCommerceCheckoutHttpHarness.checkout().run(scenario(), responder());
    }

    public static AgenticCommerceCheckoutHttpSmokeResult result() {
        return AgenticCommerceCheckoutHttpSmokeRunner.checkout().run();
    }

    private static AgenticCommerceCreateCheckoutSessionRequest createPayload() {
        return AgenticCommerceCreateCheckoutSessionRequest.fromMap(Map.of(
                "buyer",
                Map.of("first_name", "Smoke", "last_name", "Buyer", "email", "smoke@example.com"),
                "items",
                List.of(Map.of("id", "sku_smoke", "name", "Smoke Item", "quantity", 1, "unit_amount", 2500)),
                "currency",
                "usd",
                "metadata",
                Map.of("scenario", "smoke")));
    }

    private static AgenticCommerceUpdateCheckoutSessionRequest updatePayload() {
        return AgenticCommerceUpdateCheckoutSessionRequest.fromMap(Map.of(
                "fulfillment_option_id",
                "shipping_standard",
                "coupons",
                List.of("SMOKE")));
    }

    private static AgenticCommerceCompleteCheckoutSessionRequest completePayload() {
        return AgenticCommerceCompleteCheckoutSessionRequest.fromMap(Map.of(
                "payment_data",
                Map.of(
                        "handler_id",
                        "test",
                        "instrument",
                        Map.of("type", "test_payment_method", "id", "pm_smoke"))));
    }

    private static AgenticCommerceHttpResponse response(
            AgenticCommerceHttpRequest request,
            int statusCode,
            String checkoutStatus) {
        AgenticCommerceCheckoutSession session = new AgenticCommerceCheckoutSession(
                SESSION_ID,
                checkoutStatus,
                "USD",
                List.of(new AgenticCommerceLineItem(
                        "li_smoke",
                        "Smoke Item",
                        "Harness fixture item",
                        1,
                        2500,
                        2500,
                        Map.of("id", "sku_smoke"),
                        Map.of())),
                new AgenticCommerceTotals(2500, 0, 0, 0, 2500, Map.of()),
                new AgenticCommerceBuyer("Smoke", "Buyer", "smoke@example.com", "", Map.of()),
                AgenticCommerceFulfillmentDetails.empty(),
                List.of(new AgenticCommerceMessage("info", "smoke", "Smoke response.", Map.of())),
                Map.of("checkout", "https://seller.example/checkout/" + SESSION_ID),
                "",
                "",
                "",
                Map.of("fixture", true));
        return AgenticCommerceHttpResponse
                .json(statusCode, AgenticCommerceJson.write(session.toMap()))
                .withHeaders(responseHeaders(request));
    }

    private static Map<String, Object> responseHeaders(AgenticCommerceHttpRequest request) {
        String requestId = request.header(AgenticCommerceProtocol.HEADER_REQUEST_ID).orElse("smoke-response");
        String idempotencyKey = request.header(AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY).orElse("");
        return idempotencyKey.isBlank()
                ? Map.of(AgenticCommerceProtocol.HEADER_REQUEST_ID, requestId)
                : Map.of(
                        AgenticCommerceProtocol.HEADER_REQUEST_ID,
                        requestId,
                        AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY,
                        idempotencyKey);
    }
}

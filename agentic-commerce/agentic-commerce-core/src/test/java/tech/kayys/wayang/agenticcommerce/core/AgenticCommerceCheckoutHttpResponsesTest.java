package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceCheckoutHttpResponsesTest {

    @Test
    void decodesSuccessfulCheckoutSessionResponses() {
        AgenticCommerceHttpRequest request = createRequest();
        AgenticCommerceHttpResponse response = AgenticCommerceHttpResponse
                .json(
                        201,
                        """
                                {
                                  "id": "cs_123",
                                  "status": "ready_for_payment",
                                  "currency": "usd",
                                  "totals": {"subtotal": 2400, "tax": 100, "shipping": 0, "discount": 0, "total": 2500},
                                  "messages": [{"type": "info", "code": "priced", "message": "Cart priced."}]
                                }
                                """)
                .withHeaders(Map.of(
                        AgenticCommerceProtocol.HEADER_REQUEST_ID,
                        "req-1",
                        AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY,
                        "idem-1"));

        AgenticCommerceCheckoutHttpResult result = AgenticCommerceCheckoutHttpResponses.decode(request, response);

        assertThat(result.issues()).isEmpty();
        assertThat(result.valid()).isTrue();
        assertThat(result.successful()).isTrue();
        assertThat(result.hasCheckoutSession()).isTrue();
        assertThat(result.hasError()).isFalse();
        assertThat(result.checkoutSession().id()).isEqualTo("cs_123");
        assertThat(result.checkoutSession().currency()).isEqualTo("USD");
        assertThat(result.checkoutSession().totals().total()).isEqualTo(2500);
        assertThat(result.messages()).singleElement().satisfies(message -> {
            assertThat(message.code()).isEqualTo("priced");
            assertThat(message.message()).isEqualTo("Cart priced.");
        });
        assertThat(result.toMap())
                .containsEntry("valid", true)
                .containsEntry("successful", true)
                .containsEntry("operation", AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION);
    }

    @Test
    void decodesNestedCheckoutSessionBodies() {
        AgenticCommerceHttpRequest request = retrieveRequest();
        AgenticCommerceHttpResponse response = AgenticCommerceHttpResponse
                .json(
                        200,
                        """
                                {
                                  "checkout_session": {
                                    "id": "cs_nested",
                                    "status": "completed",
                                    "currency": "eur"
                                  },
                                  "request_echo": "kept"
                                }
                                """)
                .withHeaders(Map.of(AgenticCommerceProtocol.HEADER_REQUEST_ID, "req-2"));

        AgenticCommerceCheckoutHttpResult result = AgenticCommerceCheckoutHttpResponses.decode(request, response);

        assertThat(result.issues()).isEmpty();
        assertThat(result.successful()).isTrue();
        assertThat(result.checkoutSession().id()).isEqualTo("cs_nested");
        assertThat(result.checkoutSession().currency()).isEqualTo("EUR");
        assertThat(result.body()).containsEntry("request_echo", "kept");
    }

    @Test
    void decodesStructuredErrorResponsesAlongsideValidationIssues() {
        AgenticCommerceHttpResponse response = AgenticCommerceHttpResponse
                .json(
                        400,
                        """
                                {
                                  "error": {
                                    "type": "invalid_request",
                                    "code": "missing_payment_data",
                                    "message": "Payment data is required."
                                  }
                                }
                                """)
                .withHeaders(Map.of(AgenticCommerceProtocol.HEADER_REQUEST_ID, "req-3"));

        AgenticCommerceCheckoutHttpResult result = AgenticCommerceCheckoutHttpResponses.decode(completeRequest(), response);

        assertThat(result.valid()).isFalse();
        assertThat(result.successful()).isFalse();
        assertThat(result.hasError()).isTrue();
        assertThat(result.error().type()).isEqualTo("invalid_request");
        assertThat(result.error().code()).isEqualTo("missing_payment_data");
        assertThat(result.issues())
                .extracting(AgenticCommerceValidationIssue::code)
                .contains("unexpected_status");
    }

    @Test
    void reportsInvalidJsonBodiesAsDecodeIssues() {
        AgenticCommerceCheckoutHttpResult result = AgenticCommerceCheckoutHttpResponses.decode(
                retrieveRequest(),
                AgenticCommerceHttpResponse
                        .json(200, "{\"id\":")
                        .withHeaders(Map.of(AgenticCommerceProtocol.HEADER_REQUEST_ID, "req-4")));

        assertThat(result.valid()).isFalse();
        assertThat(result.successful()).isFalse();
        assertThat(result.hasCheckoutSession()).isFalse();
        assertThat(result.issues())
                .extracting(AgenticCommerceValidationIssue::code)
                .contains("invalid_json_body");
        assertThat(result.metadata())
                .containsEntry("bodyPresent", false)
                .containsEntry("routeMatched", true);
    }

    private static AgenticCommerceHttpRequest createRequest() {
        return AgenticCommerceCheckoutHttpRequests.create(
                AgenticCommerceCreateCheckoutSessionRequest.fromMap(Map.of(
                        "buyer",
                        Map.of("email", "buyer@example.com"),
                        "items",
                        java.util.List.of(Map.of("id", "sku_1", "quantity", 1)))),
                AgenticCommerceHttpRequestOptions.bearer("token")
                        .withIdempotencyKey("idem-1"));
    }

    private static AgenticCommerceHttpRequest retrieveRequest() {
        return AgenticCommerceCheckoutHttpRequests.retrieve(
                "cs_123",
                AgenticCommerceHttpRequestOptions.bearer("token"));
    }

    private static AgenticCommerceHttpRequest completeRequest() {
        return AgenticCommerceCheckoutHttpRequests.complete(
                "cs_123",
                AgenticCommerceCompleteCheckoutSessionRequest.fromMap(Map.of(
                        "payment_data",
                        Map.of("handler_id", "stripe"))),
                AgenticCommerceHttpRequestOptions.bearer("token"));
    }
}

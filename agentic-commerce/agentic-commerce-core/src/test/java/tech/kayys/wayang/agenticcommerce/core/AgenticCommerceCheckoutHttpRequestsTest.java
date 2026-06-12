package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticCommerceCheckoutHttpRequestsTest {

    private final AgenticCommerceRequestValidator validator = AgenticCommerceRequestValidator.checkout();

    @Test
    void buildsValidCreateCheckoutSessionRequests() {
        AgenticCommerceCreateCheckoutSessionRequest payload = new AgenticCommerceCreateCheckoutSessionRequest(
                new AgenticCommerceBuyer("Ada", "Lovelace", "ada@example.com", "", Map.of()),
                List.of(new AgenticCommerceCheckoutItem("sku_agent", "Agent Seat", "", 1, 1200, Map.of())),
                "usd",
                AgenticCommerceFulfillmentDetails.empty(),
                Map.of(),
                List.of(),
                Map.of(),
                List.of(),
                Map.of(),
                "",
                "",
                "",
                Map.of("tenant", "jahsy"));
        AgenticCommerceHttpRequestOptions options = AgenticCommerceHttpRequestOptions.bearer("token-1")
                .withIdempotencyKey("idem-1")
                .withRequestId("req-1")
                .withUserAgent("wayang-test")
                .withAttributes(Map.of("traceId", "trace-1"));

        AgenticCommerceHttpRequest request = AgenticCommerceCheckoutHttpRequests.create(payload, options);

        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo(AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS);
        assertThat(request.contentType(AgenticCommerceProtocol.MIME_JSON)).isTrue();
        assertThat(request.header(AgenticCommerceProtocol.HEADER_AUTHORIZATION)).contains("Bearer token-1");
        assertThat(request.header(AgenticCommerceProtocol.HEADER_API_VERSION)).contains(AgenticCommerceProtocol.SPEC_VERSION);
        assertThat(request.header(AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY)).contains("idem-1");
        assertThat(request.header(AgenticCommerceProtocol.HEADER_REQUEST_ID)).contains("req-1");
        assertThat(request.header("User-Agent")).contains("wayang-test");
        assertThat(request.attributes())
                .containsEntry("operation", AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION)
                .containsEntry("traceId", "trace-1");
        assertThat(request.body())
                .contains("\"buyer\":{\"first_name\":\"Ada\",\"last_name\":\"Lovelace\",\"email\":\"ada@example.com\"}")
                .contains("\"line_items\":[{\"id\":\"sku_agent\",\"name\":\"Agent Seat\",\"quantity\":1,\"unit_amount\":1200}]")
                .contains("\"currency\":\"USD\"");
        assertThat(validator.validate(request).valid()).isTrue();
    }

    @Test
    void buildsRetrieveRequestsWithoutBodyContentTypeAndEscapesSessionIds() {
        AgenticCommerceHttpRequest request = AgenticCommerceCheckoutHttpRequests.retrieve(
                "cs 123/part",
                AgenticCommerceHttpRequestOptions.bearer("Bearer token-1"));

        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.path()).isEqualTo("/checkout_sessions/cs%20123%2Fpart");
        assertThat(request.body()).isEmpty();
        assertThat(request.header(AgenticCommerceProtocol.HEADER_CONTENT_TYPE)).isEmpty();
        assertThat(request.header(AgenticCommerceProtocol.HEADER_AUTHORIZATION)).contains("Bearer token-1");
        assertThat(request.attributes())
                .containsEntry("operation", AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION)
                .containsEntry("checkoutSessionId", "cs 123/part");
        assertThat(validator.validate(request).valid()).isTrue();
    }

    @Test
    void buildsUpdateAndCompleteRequestsForSessionRoutes() {
        AgenticCommerceHttpRequestOptions options = AgenticCommerceHttpRequestOptions.bearer("token-1");
        AgenticCommerceHttpRequest update = AgenticCommerceCheckoutHttpRequests.update(
                "cs_123",
                AgenticCommerceUpdateCheckoutSessionRequest.fromMap(Map.of("fulfillment_option_id", "shipping_standard")),
                options);
        AgenticCommerceHttpRequest complete = AgenticCommerceCheckoutHttpRequests.complete(
                "cs_123",
                AgenticCommerceCompleteCheckoutSessionRequest.fromMap(Map.of(
                        "payment_data",
                        Map.of("handler_id", "stripe", "instrument", Map.of("id", "pm_123")))),
                options);

        assertThat(update.path()).isEqualTo("/checkout_sessions/cs_123");
        assertThat(update.body()).isEqualTo("{\"fulfillment_option_id\":\"shipping_standard\"}");
        assertThat(update.attributes()).containsEntry("operation", AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION);
        assertThat(validator.validate(update).valid()).isTrue();

        assertThat(complete.path()).isEqualTo("/checkout_sessions/cs_123/complete");
        assertThat(complete.body()).contains("\"payment_data\":{\"handler_id\":\"stripe\",\"instrument\":{\"id\":\"pm_123\"}}");
        assertThat(complete.attributes()).containsEntry("operation", AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION);
        assertThat(validator.validate(complete).valid()).isTrue();
    }

    @Test
    void buildsCancelRequestsWithOptionalBodies() {
        AgenticCommerceHttpRequestOptions options = AgenticCommerceHttpRequestOptions.bearer("token-1");
        AgenticCommerceHttpRequest emptyCancel = AgenticCommerceCheckoutHttpRequests.cancel("cs_123", options);
        AgenticCommerceHttpRequest tracedCancel = AgenticCommerceCheckoutHttpRequests.cancel(
                "cs_123",
                AgenticCommerceCancelCheckoutSessionRequest.fromMap(Map.of(
                        "intent_trace",
                        Map.of("reason_code", "buyer_declined"))),
                options);

        assertThat(emptyCancel.body()).isEmpty();
        assertThat(emptyCancel.header(AgenticCommerceProtocol.HEADER_CONTENT_TYPE)).isEmpty();
        assertThat(emptyCancel.path()).isEqualTo("/checkout_sessions/cs_123/cancel");
        assertThat(validator.validate(emptyCancel).valid()).isTrue();

        assertThat(tracedCancel.body()).isEqualTo("{\"intent_trace\":{\"reason_code\":\"buyer_declined\"}}");
        assertThat(tracedCancel.contentType(AgenticCommerceProtocol.MIME_JSON)).isTrue();
        assertThat(tracedCancel.attributes()).containsEntry("operation", AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION);
        assertThat(validator.validate(tracedCancel).valid()).isTrue();
    }

    @Test
    void rejectsBlankSessionIds() {
        assertThatThrownBy(() -> AgenticCommerceCheckoutHttpRequests.retrieve(" ", AgenticCommerceHttpRequestOptions.defaults()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checkout session id");
    }
}

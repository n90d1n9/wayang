package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceCheckoutRequestDtoTest {

    @Test
    void serializesCreateCheckoutSessionRequestsUsingProtocolKeys() {
        AgenticCommerceCreateCheckoutSessionRequest request = new AgenticCommerceCreateCheckoutSessionRequest(
                new AgenticCommerceBuyer("Ada", "Lovelace", "ada@example.com", "+12025550100", Map.of()),
                List.of(new AgenticCommerceCheckoutItem("sku_agent_seat", "Agent Seat", "", 2, 1_200, Map.of())),
                "usd",
                new AgenticCommerceFulfillmentDetails(
                        "shipping",
                        new AgenticCommerceAddress(
                                "Ada Lovelace",
                                "1 Main St",
                                "",
                                "Jakarta",
                                "DKI",
                                "10110",
                                "id",
                                Map.of()),
                        Map.of()),
                Map.of("supports_payment_data", true),
                List.of(Map.of("id", "ship_group_1", "type", "shipping")),
                Map.of("affiliate_id", "agent-1"),
                List.of("SPRING"),
                Map.of("code", "SPRING"),
                "en-US",
                "Asia/Jakarta",
                "quote_123",
                Map.of("tenant", "jahsy"));

        Map<String, Object> values = request.toMap();

        assertThat(values)
                .containsEntry("currency", "USD")
                .containsEntry("locale", "en-US")
                .containsEntry("timezone", "Asia/Jakarta")
                .containsEntry("quote_id", "quote_123");
        assertThat(values).containsKeys(
                "buyer",
                "line_items",
                "fulfillment_details",
                "capabilities",
                "fulfillment_groups",
                "affiliate_attribution",
                "coupons",
                "discounts",
                "metadata");

        List<?> lineItems = (List<?>) values.get("line_items");
        assertThat(lineItems).hasSize(1);
        assertThat(map(lineItems.get(0)))
                .containsEntry("id", "sku_agent_seat")
                .containsEntry("name", "Agent Seat")
                .containsEntry("quantity", 2L)
                .containsEntry("unit_amount", 1_200L);
    }

    @Test
    void parsesCreateRequestsFromEndpointAliasesAndPreservesExtensions() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("buyer", Map.of("firstName", "Grace", "email", "grace@example.com"));
        values.put("items", List.of(Map.of("itemId", "sku_notebook", "qty", "3", "x_catalog", "internal")));
        values.put("fulfillment_address", Map.of("line1", "9 Navy Rd", "city", "Arlington", "country", "us"));
        values.put("currency", "usd");
        values.put("x_seller_hint", "fragile");
        values.put("metadata", Map.of("channel", "agent"));

        AgenticCommerceCreateCheckoutSessionRequest request = AgenticCommerceCreateCheckoutSessionRequest.fromMap(values);

        assertThat(request.buyer().firstName()).isEqualTo("Grace");
        assertThat(request.currency()).isEqualTo("USD");
        assertThat(request.lineItems()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo("sku_notebook");
            assertThat(item.quantity()).isEqualTo(3);
            assertThat(item.metadata()).containsEntry("x_catalog", "internal");
        });
        assertThat(request.fulfillmentDetails().address().lineOne()).isEqualTo("9 Navy Rd");
        assertThat(request.fulfillmentDetails().address().country()).isEqualTo("US");
        assertThat(request.metadata())
                .containsEntry("channel", "agent")
                .containsEntry("x_seller_hint", "fragile");
        assertThat(request.toMap()).containsKeys("line_items", "fulfillment_details");
        assertThat(request.toMap()).doesNotContainKeys("items", "fulfillment_address");
    }

    @Test
    void serializesUpdateCheckoutSessionRequests() {
        AgenticCommerceUpdateCheckoutSessionRequest request = AgenticCommerceUpdateCheckoutSessionRequest.fromMap(Map.of(
                "selected_fulfillment_options",
                List.of(Map.of("id", "shipping_standard", "amount", 500)),
                "fulfillment_option_id",
                "shipping_standard",
                "coupons",
                List.of("VIP"),
                "discounts",
                Map.of("code", "VIP"),
                "x_update_reason",
                "agent_selection"));

        Map<String, Object> values = request.toMap();

        assertThat(values)
                .containsEntry("fulfillment_option_id", "shipping_standard")
                .containsEntry("coupons", List.of("VIP"));
        assertThat(values).containsKeys("selected_fulfillment_options", "discounts", "metadata");
        assertThat(request.metadata()).containsEntry("x_update_reason", "agent_selection");
    }

    @Test
    void serializesCompleteCheckoutSessionRequests() {
        AgenticCommerceCompleteCheckoutSessionRequest request = new AgenticCommerceCompleteCheckoutSessionRequest(
                AgenticCommerceBuyer.empty(),
                new AgenticCommercePaymentData(
                        "stripe",
                        Map.of("type", "payment_method", "id", "pm_123"),
                        "",
                        "",
                        Map.of("vault", "seller")),
                Map.of("three_ds", "authenticated"),
                Map.of("affiliate_id", "agent-1"),
                Map.of("score", "low"),
                Map.of("attempt", 1));

        Map<String, Object> values = request.toMap();

        assertThat(values).containsKeys(
                "payment_data",
                "authentication_result",
                "affiliate_attribution",
                "risk_signals",
                "metadata");
        assertThat(map(values.get("payment_data")))
                .containsEntry("handler_id", "stripe")
                .containsKey("instrument");
    }

    @Test
    void serializesCancelCheckoutSessionRequests() {
        AgenticCommerceCancelCheckoutSessionRequest request = AgenticCommerceCancelCheckoutSessionRequest.fromMap(Map.of(
                "intentTrace",
                Map.of("reasonCode", "buyer_changed_mind", "summary", "Agent abandoned after buyer declined shipping."),
                "metadata",
                Map.of("source", "agent")));

        Map<String, Object> values = request.toMap();

        assertThat(values).containsKeys("intent_trace", "metadata");
        assertThat(map(values.get("intent_trace")))
                .containsEntry("reason_code", "buyer_changed_mind")
                .containsEntry("trace_summary", "Agent abandoned after buyer declined shipping.");
        assertThat(request.metadata()).containsEntry("source", "agent");
    }

    @Test
    void recognizesCurrentCheckoutStatusVocabulary() {
        assertThat(AgenticCommerceCheckoutStatus.known("ready for payment")).isTrue();
        assertThat(AgenticCommerceCheckoutStatus.known("complete-in-progress")).isTrue();
        assertThat(AgenticCommerceCheckoutStatus.normalize("not ready for payment"))
                .isEqualTo(AgenticCommerceCheckoutStatus.NOT_READY_FOR_PAYMENT);
        assertThat(AgenticCommerceCheckoutStatus.normalize("authentication required"))
                .isEqualTo(AgenticCommerceCheckoutStatus.AUTHENTICATION_REQUIRED);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}

package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceCheckoutDtoTest {

    @Test
    void serializesCheckoutSessionsUsingProtocolKeys() {
        AgenticCommerceCheckoutSession session = new AgenticCommerceCheckoutSession(
                "cs_123",
                "OPEN",
                "usd",
                List.of(new AgenticCommerceLineItem(
                        "li_1",
                        "Wayang Agent Seat",
                        "Monthly agentic commerce seat",
                        2,
                        1_200,
                        2_400,
                        Map.of("id", "sku_agent_seat"),
                        Map.of("risk", "low"))),
                new AgenticCommerceTotals(2_400, 200, 0, 100, 2_500, Map.of("source", "seller")),
                new AgenticCommerceBuyer("Ada", "Lovelace", "ada@example.com", "+12025550100", Map.of("segment", "dev")),
                new AgenticCommerceFulfillmentDetails(
                        "shipping",
                        new AgenticCommerceAddress(
                                "Ada Lovelace",
                                "1 Main St",
                                "Suite 2",
                                "Jakarta",
                                "DKI",
                                "10110",
                                "id",
                                Map.of("delivery_zone", "inner")),
                        Map.of()),
                List.of(new AgenticCommerceMessage("info", "cart_priced", "Cart priced by seller.", Map.of())),
                Map.of("checkout", "https://seller.example/checkout/cs_123"),
                "https://agent.example/return",
                "https://agent.example/continue",
                "2026-06-03T00:00:00Z",
                Map.of("tenant", "jahsy"));

        Map<String, Object> values = session.toMap();

        assertThat(values)
                .containsEntry("id", "cs_123")
                .containsEntry("status", AgenticCommerceCheckoutStatus.OPEN)
                .containsEntry("currency", "USD")
                .containsEntry("return_url", "https://agent.example/return")
                .containsEntry("continue_url", "https://agent.example/continue")
                .containsEntry("expires_at", "2026-06-03T00:00:00Z");
        assertThat(values).containsKeys("line_items", "totals", "buyer", "fulfillment_details", "messages", "links", "metadata");

        List<?> lineItems = (List<?>) values.get("line_items");
        assertThat(lineItems).hasSize(1);
        Map<String, Object> firstLineItem = map(lineItems.get(0));
        assertThat(firstLineItem)
                .containsEntry("id", "li_1")
                .containsEntry("quantity", 2L)
                .containsEntry("unit_amount", 1_200L)
                .containsEntry("total_amount", 2_400L);

        Map<String, Object> totals = map(values.get("totals"));
        assertThat(totals)
                .containsEntry("subtotal", 2_400L)
                .containsEntry("tax", 200L)
                .containsEntry("discount", 100L)
                .containsEntry("total", 2_500L);

        Map<String, Object> fulfillment = map(values.get("fulfillment_details"));
        Map<String, Object> address = map(fulfillment.get("address"));
        assertThat(address)
                .containsEntry("line_one", "1 Main St")
                .containsEntry("postal_code", "10110")
                .containsEntry("country", "ID");
    }

    @Test
    void parsesCheckoutSessionsFromProtocolMapsAndPreservesExtensions() {
        Map<String, Object> lineItem = new LinkedHashMap<>();
        lineItem.put("id", "li_1");
        lineItem.put("name", "Notebook");
        lineItem.put("quantity", "2");
        lineItem.put("unit_amount", 1_100);
        lineItem.put("total_amount", 2_200L);
        lineItem.put("item", Map.of("id", "sku_notebook"));
        lineItem.put("gift_wrap", true);

        Map<String, Object> sessionMap = new LinkedHashMap<>();
        sessionMap.put("checkout_session_id", "cs_456");
        sessionMap.put("status", "Requires Action");
        sessionMap.put("currency", "usd");
        sessionMap.put("line_items", List.of(lineItem));
        sessionMap.put("totals", Map.of("subtotal", 2_200, "tax", 0, "shipping", 0, "discount", 0, "total", 2_200));
        sessionMap.put("buyer", Map.of("first_name", "Grace", "last_name", "Hopper", "email", "grace@example.com"));
        sessionMap.put(
                "fulfillment_details",
                Map.of(
                        "type",
                        "shipping",
                        "address",
                        Map.of("line_one", "9 Navy Rd", "city", "Arlington", "postal_code", "22201", "country", "us")));
        sessionMap.put("messages", List.of(Map.of("level", "warning", "code", "tax_pending", "text", "Tax may change.")));
        sessionMap.put("links", Map.of("checkout", "https://seller.example/checkout/cs_456"));
        sessionMap.put("x_future_field", "kept");
        sessionMap.put("metadata", Map.of("channel", "agent"));

        AgenticCommerceCheckoutSession session = AgenticCommerceCheckoutSession.fromMap(sessionMap);

        assertThat(session.id()).isEqualTo("cs_456");
        assertThat(session.status()).isEqualTo(AgenticCommerceCheckoutStatus.REQUIRES_ACTION);
        assertThat(session.currency()).isEqualTo("USD");
        assertThat(session.lineItems()).singleElement().satisfies(item -> {
            assertThat(item.title()).isEqualTo("Notebook");
            assertThat(item.quantity()).isEqualTo(2);
            assertThat(item.unitAmount()).isEqualTo(1_100);
            assertThat(item.totalAmount()).isEqualTo(2_200);
            assertThat(item.metadata()).containsEntry("gift_wrap", true);
        });
        assertThat(session.buyer().email()).isEqualTo("grace@example.com");
        assertThat(session.fulfillmentDetails().address().country()).isEqualTo("US");
        assertThat(session.messages()).singleElement().satisfies(message -> {
            assertThat(message.type()).isEqualTo("warning");
            assertThat(message.message()).isEqualTo("Tax may change.");
        });
        assertThat(session.metadata())
                .containsEntry("channel", "agent")
                .containsEntry("x_future_field", "kept");
        assertThat(session.toMap()).containsKeys("id", "status", "currency", "line_items", "totals");
    }

    @Test
    void normalizesCheckoutStatusValues() {
        assertThat(AgenticCommerceCheckoutStatus.normalize("Requires Action"))
                .isEqualTo(AgenticCommerceCheckoutStatus.REQUIRES_ACTION);
        assertThat(AgenticCommerceCheckoutStatus.normalize("cancelled"))
                .isEqualTo(AgenticCommerceCheckoutStatus.CANCELED);
        assertThat(AgenticCommerceCheckoutStatus.normalize(""))
                .isEqualTo(AgenticCommerceCheckoutStatus.UNKNOWN);
        assertThat(AgenticCommerceCheckoutStatus.known("expired")).isTrue();
        assertThat(AgenticCommerceCheckoutStatus.known("seller_review")).isFalse();
    }

    @Test
    void serializesAndParsesErrorPayloads() {
        AgenticCommerceError error = AgenticCommerceError.fromMap(Map.of(
                "error",
                Map.of(
                        "type",
                        "invalid_request",
                        "code",
                        "missing_line_items",
                        "message",
                        "At least one line item is required.",
                        "details",
                        Map.of("field", "line_items"),
                        "x_retryable",
                        false)));

        assertThat(error.type()).isEqualTo("invalid_request");
        assertThat(error.code()).isEqualTo("missing_line_items");
        assertThat(error.details()).containsEntry("field", "line_items");
        assertThat(error.metadata()).containsEntry("x_retryable", false);
        assertThat(error.toMap())
                .containsEntry("type", "invalid_request")
                .containsEntry("message", "At least one line item is required.");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}

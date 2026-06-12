package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dependency-free checkout session contract for Agentic Commerce adapters.
 */
public record AgenticCommerceCheckoutSession(
        String id,
        String status,
        String currency,
        List<AgenticCommerceLineItem> lineItems,
        AgenticCommerceTotals totals,
        AgenticCommerceBuyer buyer,
        AgenticCommerceFulfillmentDetails fulfillmentDetails,
        List<AgenticCommerceMessage> messages,
        Map<String, Object> links,
        String returnUrl,
        String continueUrl,
        String expiresAt,
        Map<String, Object> metadata) {

    public AgenticCommerceCheckoutSession {
        id = AgenticCommerceValues.textValue(id);
        status = AgenticCommerceCheckoutStatus.normalizeOptional(status);
        currency = AgenticCommerceValues.uppercaseText(currency);
        lineItems = lineItems == null
                ? List.of()
                : lineItems.stream()
                        .filter(item -> item != null && !item.isEmpty())
                        .toList();
        totals = totals == null ? AgenticCommerceTotals.empty() : totals;
        buyer = buyer == null ? AgenticCommerceBuyer.empty() : buyer;
        fulfillmentDetails = fulfillmentDetails == null
                ? AgenticCommerceFulfillmentDetails.empty()
                : fulfillmentDetails;
        messages = messages == null
                ? List.of()
                : messages.stream()
                        .filter(message -> message != null && !message.isEmpty())
                        .toList();
        links = AgenticCommerceMaps.copy(links);
        returnUrl = AgenticCommerceValues.textValue(returnUrl);
        continueUrl = AgenticCommerceValues.textValue(continueUrl);
        expiresAt = AgenticCommerceValues.textValue(expiresAt);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceCheckoutSession empty() {
        return new AgenticCommerceCheckoutSession(
                "",
                "",
                "",
                List.of(),
                AgenticCommerceTotals.empty(),
                AgenticCommerceBuyer.empty(),
                AgenticCommerceFulfillmentDetails.empty(),
                List.of(),
                Map.of(),
                "",
                "",
                "",
                Map.of());
    }

    public static AgenticCommerceCheckoutSession fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return empty();
        }
        return new AgenticCommerceCheckoutSession(
                AgenticCommerceValues.text(values, "id", "checkout_session_id", "checkoutSessionId"),
                AgenticCommerceValues.text(values, "status"),
                AgenticCommerceValues.text(values, "currency"),
                AgenticCommerceValues.maps(values, "line_items", "lineItems", "items").stream()
                        .map(AgenticCommerceLineItem::fromMap)
                        .toList(),
                AgenticCommerceTotals.fromMap(AgenticCommerceValues.map(values, "totals")),
                AgenticCommerceBuyer.fromMap(AgenticCommerceValues.map(values, "buyer")),
                AgenticCommerceFulfillmentDetails.fromMap(AgenticCommerceValues.map(
                        values,
                        "fulfillment_details",
                        "fulfillmentDetails")),
                AgenticCommerceValues.maps(values, "messages").stream()
                        .map(AgenticCommerceMessage::fromMap)
                        .toList(),
                AgenticCommerceValues.map(values, "links"),
                AgenticCommerceValues.text(values, "return_url", "returnUrl"),
                AgenticCommerceValues.text(values, "continue_url", "continueUrl"),
                AgenticCommerceValues.text(values, "expires_at", "expiresAt"),
                AgenticCommerceValues.metadata(
                        values,
                        "id",
                        "checkout_session_id",
                        "checkoutSessionId",
                        "status",
                        "currency",
                        "line_items",
                        "lineItems",
                        "items",
                        "totals",
                        "buyer",
                        "fulfillment_details",
                        "fulfillmentDetails",
                        "messages",
                        "links",
                        "return_url",
                        "returnUrl",
                        "continue_url",
                        "continueUrl",
                        "expires_at",
                        "expiresAt"));
    }

    public boolean isEmpty() {
        return id.isBlank()
                && status.isBlank()
                && currency.isBlank()
                && lineItems.isEmpty()
                && totals.isEmpty()
                && buyer.isEmpty()
                && fulfillmentDetails.isEmpty()
                && messages.isEmpty()
                && links.isEmpty()
                && returnUrl.isBlank()
                && continueUrl.isBlank()
                && expiresAt.isBlank()
                && metadata.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        AgenticCommerceValues.putText(values, "id", id);
        AgenticCommerceValues.putText(values, "status", status);
        AgenticCommerceValues.putText(values, "currency", currency);
        AgenticCommerceValues.putList(values, "line_items", lineItems.stream()
                .map(AgenticCommerceLineItem::toMap)
                .toList());
        if (!totals.isEmpty()) {
            values.put("totals", totals.toMap());
        }
        if (!buyer.isEmpty()) {
            values.put("buyer", buyer.toMap());
        }
        if (!fulfillmentDetails.isEmpty()) {
            values.put("fulfillment_details", fulfillmentDetails.toMap());
        }
        AgenticCommerceValues.putList(values, "messages", messages.stream()
                .map(AgenticCommerceMessage::toMap)
                .toList());
        AgenticCommerceValues.putMap(values, "links", links);
        AgenticCommerceValues.putText(values, "return_url", returnUrl);
        AgenticCommerceValues.putText(values, "continue_url", continueUrl);
        AgenticCommerceValues.putText(values, "expires_at", expiresAt);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}

package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request body for creating an Agentic Commerce checkout session.
 */
public record AgenticCommerceCreateCheckoutSessionRequest(
        AgenticCommerceBuyer buyer,
        List<AgenticCommerceCheckoutItem> lineItems,
        String currency,
        AgenticCommerceFulfillmentDetails fulfillmentDetails,
        Map<String, Object> capabilities,
        List<Map<String, Object>> fulfillmentGroups,
        Map<String, Object> affiliateAttribution,
        List<String> coupons,
        Map<String, Object> discounts,
        String locale,
        String timezone,
        String quoteId,
        Map<String, Object> metadata) implements AgenticCommerceCheckoutPayload {

    public AgenticCommerceCreateCheckoutSessionRequest {
        buyer = buyer == null ? AgenticCommerceBuyer.empty() : buyer;
        lineItems = lineItems == null
                ? List.of()
                : lineItems.stream()
                        .filter(item -> item != null && !item.isEmpty())
                        .toList();
        currency = AgenticCommerceValues.uppercaseText(currency);
        fulfillmentDetails = fulfillmentDetails == null
                ? AgenticCommerceFulfillmentDetails.empty()
                : fulfillmentDetails;
        capabilities = AgenticCommerceMaps.copy(capabilities);
        fulfillmentGroups = AgenticCommerceMaps.copyMaps(fulfillmentGroups);
        affiliateAttribution = AgenticCommerceMaps.copy(affiliateAttribution);
        coupons = AgenticCommerceValues.strings(coupons);
        discounts = AgenticCommerceMaps.copy(discounts);
        locale = AgenticCommerceValues.textValue(locale);
        timezone = AgenticCommerceValues.textValue(timezone);
        quoteId = AgenticCommerceValues.textValue(quoteId);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceCreateCheckoutSessionRequest fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return new AgenticCommerceCreateCheckoutSessionRequest(
                    AgenticCommerceBuyer.empty(),
                    List.of(),
                    "",
                    AgenticCommerceFulfillmentDetails.empty(),
                    Map.of(),
                    List.of(),
                    Map.of(),
                    List.of(),
                    Map.of(),
                    "",
                    "",
                    "",
                    Map.of());
        }
        return new AgenticCommerceCreateCheckoutSessionRequest(
                AgenticCommerceBuyer.fromMap(AgenticCommerceValues.map(values, "buyer")),
                checkoutItems(values),
                AgenticCommerceValues.text(values, "currency"),
                fulfillmentDetails(values),
                AgenticCommerceValues.map(values, "capabilities"),
                AgenticCommerceValues.maps(values, "fulfillment_groups", "fulfillmentGroups"),
                AgenticCommerceValues.map(values, "affiliate_attribution", "affiliateAttribution"),
                AgenticCommerceValues.stringList(values, "coupons"),
                AgenticCommerceValues.map(values, "discounts"),
                AgenticCommerceValues.text(values, "locale"),
                AgenticCommerceValues.text(values, "timezone"),
                AgenticCommerceValues.text(values, "quote_id", "quoteId"),
                AgenticCommerceValues.metadata(
                        values,
                        "buyer",
                        "line_items",
                        "lineItems",
                        "items",
                        "currency",
                        "fulfillment_details",
                        "fulfillmentDetails",
                        "fulfillment_address",
                        "fulfillmentAddress",
                        "capabilities",
                        "fulfillment_groups",
                        "fulfillmentGroups",
                        "affiliate_attribution",
                        "affiliateAttribution",
                        "coupons",
                        "discounts",
                        "locale",
                        "timezone",
                        "quote_id",
                        "quoteId"));
    }

    @Override
    public boolean isEmpty() {
        return buyer.isEmpty()
                && lineItems.isEmpty()
                && currency.isBlank()
                && fulfillmentDetails.isEmpty()
                && capabilities.isEmpty()
                && fulfillmentGroups.isEmpty()
                && affiliateAttribution.isEmpty()
                && coupons.isEmpty()
                && discounts.isEmpty()
                && locale.isBlank()
                && timezone.isBlank()
                && quoteId.isBlank()
                && metadata.isEmpty();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        if (!buyer.isEmpty()) {
            values.put("buyer", buyer.toMap());
        }
        AgenticCommerceValues.putList(values, "line_items", lineItems.stream()
                .map(AgenticCommerceCheckoutItem::toMap)
                .toList());
        AgenticCommerceValues.putText(values, "currency", currency);
        if (!fulfillmentDetails.isEmpty()) {
            values.put("fulfillment_details", fulfillmentDetails.toMap());
        }
        AgenticCommerceValues.putMap(values, "capabilities", capabilities);
        AgenticCommerceValues.putList(values, "fulfillment_groups", fulfillmentGroups);
        AgenticCommerceValues.putMap(values, "affiliate_attribution", affiliateAttribution);
        AgenticCommerceValues.putStringList(values, "coupons", coupons);
        AgenticCommerceValues.putMap(values, "discounts", discounts);
        AgenticCommerceValues.putText(values, "locale", locale);
        AgenticCommerceValues.putText(values, "timezone", timezone);
        AgenticCommerceValues.putText(values, "quote_id", quoteId);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }

    private static List<AgenticCommerceCheckoutItem> checkoutItems(Map<?, ?> values) {
        return AgenticCommerceValues.maps(values, "line_items", "lineItems", "items").stream()
                .map(AgenticCommerceCheckoutItem::fromMap)
                .toList();
    }

    private static AgenticCommerceFulfillmentDetails fulfillmentDetails(Map<?, ?> values) {
        Map<String, Object> details = AgenticCommerceValues.map(values, "fulfillment_details", "fulfillmentDetails");
        if (!details.isEmpty()) {
            return AgenticCommerceFulfillmentDetails.fromMap(details);
        }
        Map<String, Object> address = AgenticCommerceValues.map(values, "fulfillment_address", "fulfillmentAddress");
        if (!address.isEmpty()) {
            return new AgenticCommerceFulfillmentDetails("", AgenticCommerceAddress.fromMap(address), Map.of());
        }
        return AgenticCommerceFulfillmentDetails.empty();
    }
}

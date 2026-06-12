package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request body for updating an Agentic Commerce checkout session.
 */
public record AgenticCommerceUpdateCheckoutSessionRequest(
        AgenticCommerceBuyer buyer,
        List<AgenticCommerceCheckoutItem> lineItems,
        AgenticCommerceFulfillmentDetails fulfillmentDetails,
        List<Map<String, Object>> fulfillmentGroups,
        List<Map<String, Object>> selectedFulfillmentOptions,
        String fulfillmentOptionId,
        List<String> coupons,
        Map<String, Object> discounts,
        Map<String, Object> metadata) implements AgenticCommerceCheckoutPayload {

    public AgenticCommerceUpdateCheckoutSessionRequest {
        buyer = buyer == null ? AgenticCommerceBuyer.empty() : buyer;
        lineItems = lineItems == null
                ? List.of()
                : lineItems.stream()
                        .filter(item -> item != null && !item.isEmpty())
                        .toList();
        fulfillmentDetails = fulfillmentDetails == null
                ? AgenticCommerceFulfillmentDetails.empty()
                : fulfillmentDetails;
        fulfillmentGroups = AgenticCommerceMaps.copyMaps(fulfillmentGroups);
        selectedFulfillmentOptions = AgenticCommerceMaps.copyMaps(selectedFulfillmentOptions);
        fulfillmentOptionId = AgenticCommerceValues.textValue(fulfillmentOptionId);
        coupons = AgenticCommerceValues.strings(coupons);
        discounts = AgenticCommerceMaps.copy(discounts);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceUpdateCheckoutSessionRequest fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return new AgenticCommerceUpdateCheckoutSessionRequest(
                    AgenticCommerceBuyer.empty(),
                    List.of(),
                    AgenticCommerceFulfillmentDetails.empty(),
                    List.of(),
                    List.of(),
                    "",
                    List.of(),
                    Map.of(),
                    Map.of());
        }
        return new AgenticCommerceUpdateCheckoutSessionRequest(
                AgenticCommerceBuyer.fromMap(AgenticCommerceValues.map(values, "buyer")),
                AgenticCommerceValues.maps(values, "line_items", "lineItems", "items").stream()
                        .map(AgenticCommerceCheckoutItem::fromMap)
                        .toList(),
                fulfillmentDetails(values),
                AgenticCommerceValues.maps(values, "fulfillment_groups", "fulfillmentGroups"),
                AgenticCommerceValues.maps(values, "selected_fulfillment_options", "selectedFulfillmentOptions"),
                AgenticCommerceValues.text(values, "fulfillment_option_id", "fulfillmentOptionId"),
                AgenticCommerceValues.stringList(values, "coupons"),
                AgenticCommerceValues.map(values, "discounts"),
                AgenticCommerceValues.metadata(
                        values,
                        "buyer",
                        "line_items",
                        "lineItems",
                        "items",
                        "fulfillment_details",
                        "fulfillmentDetails",
                        "fulfillment_address",
                        "fulfillmentAddress",
                        "fulfillment_groups",
                        "fulfillmentGroups",
                        "selected_fulfillment_options",
                        "selectedFulfillmentOptions",
                        "fulfillment_option_id",
                        "fulfillmentOptionId",
                        "coupons",
                        "discounts"));
    }

    @Override
    public boolean isEmpty() {
        return buyer.isEmpty()
                && lineItems.isEmpty()
                && fulfillmentDetails.isEmpty()
                && fulfillmentGroups.isEmpty()
                && selectedFulfillmentOptions.isEmpty()
                && fulfillmentOptionId.isBlank()
                && coupons.isEmpty()
                && discounts.isEmpty()
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
        if (!fulfillmentDetails.isEmpty()) {
            values.put("fulfillment_details", fulfillmentDetails.toMap());
        }
        AgenticCommerceValues.putList(values, "fulfillment_groups", fulfillmentGroups);
        AgenticCommerceValues.putList(values, "selected_fulfillment_options", selectedFulfillmentOptions);
        AgenticCommerceValues.putText(values, "fulfillment_option_id", fulfillmentOptionId);
        AgenticCommerceValues.putStringList(values, "coupons", coupons);
        AgenticCommerceValues.putMap(values, "discounts", discounts);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
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

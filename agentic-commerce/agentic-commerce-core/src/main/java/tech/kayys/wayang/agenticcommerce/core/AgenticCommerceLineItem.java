package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Checkout line item with protocol integer minor-unit amounts.
 */
public record AgenticCommerceLineItem(
        String id,
        String title,
        String description,
        long quantity,
        long unitAmount,
        long totalAmount,
        Map<String, Object> item,
        Map<String, Object> metadata) {

    public AgenticCommerceLineItem {
        id = AgenticCommerceValues.textValue(id);
        title = AgenticCommerceValues.textValue(title);
        description = AgenticCommerceValues.textValue(description);
        quantity = Math.max(0, quantity);
        unitAmount = Math.max(0, unitAmount);
        totalAmount = Math.max(0, totalAmount);
        item = AgenticCommerceMaps.copy(item);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceLineItem fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return new AgenticCommerceLineItem("", "", "", 0, 0, 0, Map.of(), Map.of());
        }
        return new AgenticCommerceLineItem(
                AgenticCommerceValues.text(values, "id", "line_item_id", "lineItemId"),
                AgenticCommerceValues.text(values, "title", "name"),
                AgenticCommerceValues.text(values, "description"),
                AgenticCommerceValues.longValue(values, "quantity", "qty"),
                AgenticCommerceValues.longValue(values, "unit_amount", "unitAmount"),
                AgenticCommerceValues.longValue(values, "total_amount", "totalAmount", "amount"),
                AgenticCommerceValues.map(values, "item"),
                AgenticCommerceValues.metadata(
                        values,
                        "id",
                        "line_item_id",
                        "lineItemId",
                        "title",
                        "name",
                        "description",
                        "quantity",
                        "qty",
                        "unit_amount",
                        "unitAmount",
                        "total_amount",
                        "totalAmount",
                        "amount",
                        "item"));
    }

    public boolean isEmpty() {
        return id.isBlank()
                && title.isBlank()
                && description.isBlank()
                && quantity == 0
                && unitAmount == 0
                && totalAmount == 0
                && item.isEmpty()
                && metadata.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        AgenticCommerceValues.putText(values, "id", id);
        AgenticCommerceValues.putText(values, "title", title);
        AgenticCommerceValues.putText(values, "description", description);
        if (quantity > 0) {
            values.put("quantity", quantity);
        }
        if (unitAmount > 0) {
            values.put("unit_amount", unitAmount);
        }
        if (totalAmount > 0) {
            values.put("total_amount", totalAmount);
        }
        AgenticCommerceValues.putMap(values, "item", item);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}

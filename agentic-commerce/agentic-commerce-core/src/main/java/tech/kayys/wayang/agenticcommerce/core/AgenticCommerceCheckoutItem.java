package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Item requested by an agent when creating or updating a checkout session.
 */
public record AgenticCommerceCheckoutItem(
        String id,
        String name,
        String description,
        long quantity,
        long unitAmount,
        Map<String, Object> metadata) {

    public AgenticCommerceCheckoutItem {
        id = AgenticCommerceValues.textValue(id);
        name = AgenticCommerceValues.textValue(name);
        description = AgenticCommerceValues.textValue(description);
        quantity = Math.max(0, quantity);
        unitAmount = Math.max(0, unitAmount);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceCheckoutItem of(String id, long quantity) {
        return new AgenticCommerceCheckoutItem(id, "", "", quantity, 0, Map.of());
    }

    public static AgenticCommerceCheckoutItem fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return new AgenticCommerceCheckoutItem("", "", "", 0, 0, Map.of());
        }
        return new AgenticCommerceCheckoutItem(
                AgenticCommerceValues.text(values, "id", "item_id", "itemId", "product_id", "productId"),
                AgenticCommerceValues.text(values, "name", "title"),
                AgenticCommerceValues.text(values, "description"),
                AgenticCommerceValues.longValue(values, "quantity", "qty"),
                AgenticCommerceValues.longValue(values, "unit_amount", "unitAmount"),
                AgenticCommerceValues.metadata(
                        values,
                        "id",
                        "item_id",
                        "itemId",
                        "product_id",
                        "productId",
                        "name",
                        "title",
                        "description",
                        "quantity",
                        "qty",
                        "unit_amount",
                        "unitAmount"));
    }

    public boolean isEmpty() {
        return id.isBlank()
                && name.isBlank()
                && description.isBlank()
                && quantity == 0
                && unitAmount == 0
                && metadata.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        AgenticCommerceValues.putText(values, "id", id);
        AgenticCommerceValues.putText(values, "name", name);
        AgenticCommerceValues.putText(values, "description", description);
        if (quantity > 0) {
            values.put("quantity", quantity);
        }
        if (unitAmount > 0) {
            values.put("unit_amount", unitAmount);
        }
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}

package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shipping or delivery details attached to a checkout session.
 */
public record AgenticCommerceFulfillmentDetails(
        String type,
        AgenticCommerceAddress address,
        Map<String, Object> metadata) {

    public AgenticCommerceFulfillmentDetails {
        type = AgenticCommerceValues.textValue(type);
        address = address == null ? AgenticCommerceAddress.empty() : address;
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceFulfillmentDetails empty() {
        return new AgenticCommerceFulfillmentDetails("", AgenticCommerceAddress.empty(), Map.of());
    }

    public static AgenticCommerceFulfillmentDetails fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return empty();
        }
        return new AgenticCommerceFulfillmentDetails(
                AgenticCommerceValues.text(values, "type"),
                AgenticCommerceAddress.fromMap(AgenticCommerceValues.map(values, "address")),
                AgenticCommerceValues.metadata(values, "type", "address"));
    }

    public boolean isEmpty() {
        return type.isBlank() && address.isEmpty() && metadata.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        AgenticCommerceValues.putText(values, "type", type);
        if (!address.isEmpty()) {
            values.put("address", address.toMap());
        }
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}

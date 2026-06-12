package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cart totals represented as integer minor-unit amounts in the session currency.
 */
public record AgenticCommerceTotals(
        long subtotal,
        long tax,
        long shipping,
        long discount,
        long total,
        Map<String, Object> metadata) {

    public AgenticCommerceTotals {
        subtotal = Math.max(0, subtotal);
        tax = Math.max(0, tax);
        shipping = Math.max(0, shipping);
        discount = Math.max(0, discount);
        total = Math.max(0, total);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceTotals empty() {
        return new AgenticCommerceTotals(0, 0, 0, 0, 0, Map.of());
    }

    public static AgenticCommerceTotals fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return empty();
        }
        return new AgenticCommerceTotals(
                AgenticCommerceValues.longValue(values, "subtotal", "subtotal_amount", "subtotalAmount"),
                AgenticCommerceValues.longValue(values, "tax", "tax_amount", "taxAmount"),
                AgenticCommerceValues.longValue(values, "shipping", "shipping_amount", "shippingAmount"),
                AgenticCommerceValues.longValue(values, "discount", "discount_amount", "discountAmount"),
                AgenticCommerceValues.longValue(values, "total", "total_amount", "totalAmount"),
                AgenticCommerceValues.metadata(
                        values,
                        "subtotal",
                        "subtotal_amount",
                        "subtotalAmount",
                        "tax",
                        "tax_amount",
                        "taxAmount",
                        "shipping",
                        "shipping_amount",
                        "shippingAmount",
                        "discount",
                        "discount_amount",
                        "discountAmount",
                        "total",
                        "total_amount",
                        "totalAmount"));
    }

    public boolean isEmpty() {
        return subtotal == 0
                && tax == 0
                && shipping == 0
                && discount == 0
                && total == 0
                && metadata.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("subtotal", subtotal);
        values.put("tax", tax);
        values.put("shipping", shipping);
        values.put("discount", discount);
        values.put("total", total);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}

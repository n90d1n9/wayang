package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Buyer identity details for Agentic Commerce checkout sessions.
 */
public record AgenticCommerceBuyer(
        String firstName,
        String lastName,
        String email,
        String phone,
        Map<String, Object> metadata) {

    public AgenticCommerceBuyer {
        firstName = AgenticCommerceValues.textValue(firstName);
        lastName = AgenticCommerceValues.textValue(lastName);
        email = AgenticCommerceValues.textValue(email);
        phone = AgenticCommerceValues.textValue(phone);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceBuyer empty() {
        return new AgenticCommerceBuyer("", "", "", "", Map.of());
    }

    public static AgenticCommerceBuyer fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return empty();
        }
        return new AgenticCommerceBuyer(
                AgenticCommerceValues.text(values, "first_name", "firstName"),
                AgenticCommerceValues.text(values, "last_name", "lastName"),
                AgenticCommerceValues.text(values, "email"),
                AgenticCommerceValues.text(values, "phone"),
                AgenticCommerceValues.metadata(
                        values,
                        "first_name",
                        "firstName",
                        "last_name",
                        "lastName",
                        "email",
                        "phone"));
    }

    public boolean isEmpty() {
        return firstName.isBlank()
                && lastName.isBlank()
                && email.isBlank()
                && phone.isBlank()
                && metadata.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        AgenticCommerceValues.putText(values, "first_name", firstName);
        AgenticCommerceValues.putText(values, "last_name", lastName);
        AgenticCommerceValues.putText(values, "email", email);
        AgenticCommerceValues.putText(values, "phone", phone);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}

package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Protocol-shaped postal address used by checkout fulfillment details.
 */
public record AgenticCommerceAddress(
        String name,
        String lineOne,
        String lineTwo,
        String city,
        String state,
        String postalCode,
        String country,
        Map<String, Object> metadata) {

    public AgenticCommerceAddress {
        name = AgenticCommerceValues.textValue(name);
        lineOne = AgenticCommerceValues.textValue(lineOne);
        lineTwo = AgenticCommerceValues.textValue(lineTwo);
        city = AgenticCommerceValues.textValue(city);
        state = AgenticCommerceValues.textValue(state);
        postalCode = AgenticCommerceValues.textValue(postalCode);
        country = AgenticCommerceValues.uppercaseText(country);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceAddress empty() {
        return new AgenticCommerceAddress("", "", "", "", "", "", "", Map.of());
    }

    public static AgenticCommerceAddress fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return empty();
        }
        return new AgenticCommerceAddress(
                AgenticCommerceValues.text(values, "name"),
                AgenticCommerceValues.text(values, "line_one", "lineOne", "line1"),
                AgenticCommerceValues.text(values, "line_two", "lineTwo", "line2"),
                AgenticCommerceValues.text(values, "city"),
                AgenticCommerceValues.text(values, "state", "region", "province"),
                AgenticCommerceValues.text(values, "postal_code", "postalCode", "zip"),
                AgenticCommerceValues.text(values, "country"),
                AgenticCommerceValues.metadata(
                        values,
                        "name",
                        "line_one",
                        "lineOne",
                        "line1",
                        "line_two",
                        "lineTwo",
                        "line2",
                        "city",
                        "state",
                        "region",
                        "province",
                        "postal_code",
                        "postalCode",
                        "zip",
                        "country"));
    }

    public boolean isEmpty() {
        return name.isBlank()
                && lineOne.isBlank()
                && lineTwo.isBlank()
                && city.isBlank()
                && state.isBlank()
                && postalCode.isBlank()
                && country.isBlank()
                && metadata.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        AgenticCommerceValues.putText(values, "name", name);
        AgenticCommerceValues.putText(values, "line_one", lineOne);
        AgenticCommerceValues.putText(values, "line_two", lineTwo);
        AgenticCommerceValues.putText(values, "city", city);
        AgenticCommerceValues.putText(values, "state", state);
        AgenticCommerceValues.putText(values, "postal_code", postalCode);
        AgenticCommerceValues.putText(values, "country", country);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}

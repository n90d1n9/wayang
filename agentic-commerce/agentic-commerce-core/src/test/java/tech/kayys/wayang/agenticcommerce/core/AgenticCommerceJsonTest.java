package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticCommerceJsonTest {

    @Test
    void writesOrderedNestedJsonAndEscapesStrings() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", "cs_123");
        values.put("message", "Quote \"ready\"\nnext");
        values.put("amount", 2500L);
        values.put("active", true);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", "sku_1");
        item.put("quantity", 2);
        values.put("items", List.of(item));

        assertThat(AgenticCommerceJson.write(values))
                .isEqualTo("{\"id\":\"cs_123\",\"message\":\"Quote \\\"ready\\\"\\nnext\","
                        + "\"amount\":2500,\"active\":true,\"items\":[{\"id\":\"sku_1\",\"quantity\":2}]}");
    }

    @Test
    void writesNullAndNonFiniteNumbersSafely() {
        assertThat(AgenticCommerceJson.write(null)).isEqualTo("null");
        assertThat(AgenticCommerceJson.write(Double.NaN)).isEqualTo("0");
        assertThat(AgenticCommerceJson.write(Map.of("future", new Object() {
            @Override
            public String toString() {
                return "extension";
            }
        }))).isEqualTo("{\"future\":\"extension\"}");
    }

    @Test
    void readsNestedJsonObjects() {
        Map<String, Object> values = AgenticCommerceJson.readObject("""
                {
                  "id": "cs_123",
                  "amount": 2500,
                  "tax": 12.5,
                  "active": true,
                  "empty": null,
                  "message": "Line\\nTwo",
                  "items": [{"id": "sku_1", "quantity": 2}]
                }
                """);

        assertThat(values)
                .containsEntry("id", "cs_123")
                .containsEntry("amount", 2500L)
                .containsEntry("tax", 12.5d)
                .containsEntry("active", true)
                .containsEntry("message", "Line\nTwo")
                .doesNotContainKey("empty");
        assertThat((List<?>) values.get("items")).hasSize(1);
    }

    @Test
    void rejectsBlankInvalidAndNonObjectJsonPayloads() {
        assertThatThrownBy(() -> AgenticCommerceJson.read(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        assertThatThrownBy(() -> AgenticCommerceJson.readObject("[1]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("object");
        assertThatThrownBy(() -> AgenticCommerceJson.readObject("{\"id\":"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unexpected end");
    }
}

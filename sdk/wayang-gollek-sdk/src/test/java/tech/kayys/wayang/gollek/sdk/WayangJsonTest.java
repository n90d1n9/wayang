package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangJsonTest {

    @Test
    void rendersOrderedEnvelopeMapsAsCompactJson() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", "Wayang");
        values.put("enabled", true);
        values.put("count", 2);
        values.put("items", List.of("cli", "sdk"));

        assertThat(WayangJson.object(values))
                .isEqualTo("{\"product\":\"Wayang\",\"enabled\":true,\"count\":2,\"items\":[\"cli\",\"sdk\"]}");
    }

    @Test
    void escapesJsonStringContent() {
        assertThat(WayangJson.quote("line\nquote\"tab\tbackslash\\control" + (char) 0x01))
                .isEqualTo("\"line\\nquote\\\"tab\\tbackslash\\\\control\\u0001\"");
    }

    @Test
    void rendersNestedMapsIterablesAndArrays() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("numbers", new int[]{1, 2, 3});
        nested.put("objects", List.of(Map.of("name", "agent")));

        assertThat(WayangJson.value(nested))
                .isEqualTo("{\"numbers\":[1,2,3],\"objects\":[{\"name\":\"agent\"}]}");
    }

    @Test
    void keepsNonFiniteNumbersValidJsonByRenderingThemAsStrings() {
        assertThat(WayangJson.value(Double.NaN)).isEqualTo("\"NaN\"");
        assertThat(WayangJson.value(Double.POSITIVE_INFINITY)).isEqualTo("\"Infinity\"");
        assertThat(WayangJson.value(Float.NEGATIVE_INFINITY)).isEqualTo("\"-Infinity\"");
    }
}

package tech.kayys.wayang.a2ui.wayang.support;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringMapsTest {

    @Test
    void copyStringValuesTrimsKeysAndPreservesValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(" tenantId ", " tenant-a ");
        values.put(" ", "ignored");
        values.put(null, "ignored");
        values.put("nullable", null);

        Map<String, String> normalized = StringMaps.copyStringValues(values);

        assertThat(normalized)
                .containsEntry("tenantId", " tenant-a ")
                .doesNotContainKeys("", "nullable");

        values.put("later", "change");
        assertThat(normalized).doesNotContainKey("later");
        assertThatThrownBy(() -> normalized.put("blocked", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void stringValuesCoercesGenericMapValues() {
        Map<Object, Object> values = new LinkedHashMap<>();
        values.put(7, 42);
        values.put(" surfaceId ", " surface-1 ");
        values.put("nullable", null);
        values.put(null, "ignored");

        Map<String, String> normalized = StringMaps.stringValues(values);

        assertThat(normalized)
                .containsEntry("7", "42")
                .containsEntry("surfaceId", " surface-1 ")
                .doesNotContainKeys("nullable", "null");
    }

    @Test
    void treatsNullAndEmptyInputsAsEmpty() {
        assertThat(StringMaps.copyStringValues(null)).isEmpty();
        assertThat(StringMaps.copyStringValues(Map.of())).isEmpty();
        assertThat(StringMaps.stringValues(null)).isEmpty();
        assertThat(StringMaps.stringValues(Map.of())).isEmpty();
        assertThat(StringMaps.stringValues("not-a-map")).isEmpty();
    }
}

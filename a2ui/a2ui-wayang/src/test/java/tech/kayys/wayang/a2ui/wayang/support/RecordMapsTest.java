package tech.kayys.wayang.a2ui.wayang.support;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordMapsTest {

    @Test
    void nullableValuesPreservesNullValuesAndSkipsNullKeys() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("present", "value");
        values.put("nullable", null);
        values.put(null, "ignored");

        Map<String, Object> normalized = RecordMaps.nullableValues(values);

        assertThat(normalized)
                .containsEntry("present", "value")
                .containsEntry("nullable", null)
                .doesNotContainKey(null);

        values.put("later", "change");
        assertThat(normalized).doesNotContainKey("later");
        assertThatThrownBy(() -> normalized.put("blocked", true))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void stringKeysNonNullValuesStringifiesKeysAndDropsNullValues() {
        Map<Object, Object> values = new LinkedHashMap<>();
        values.put(7, "seven");
        values.put("nullable", null);
        values.put(null, "ignored");

        Map<String, Object> normalized = RecordMaps.stringKeysNonNullValues(values);

        assertThat(normalized)
                .containsEntry("7", "seven")
                .doesNotContainKey("nullable")
                .doesNotContainKey(null);
    }

    @Test
    void treatsNullAndEmptyMapsAsEmpty() {
        assertThat(RecordMaps.nullableValues(null)).isEmpty();
        assertThat(RecordMaps.nullableValues(Map.of())).isEmpty();
        assertThat(RecordMaps.stringKeysNonNullValues(null)).isEmpty();
        assertThat(RecordMaps.stringKeysNonNullValues(Map.of())).isEmpty();
    }
}

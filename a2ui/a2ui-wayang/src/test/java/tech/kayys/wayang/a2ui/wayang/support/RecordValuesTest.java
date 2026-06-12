package tech.kayys.wayang.a2ui.wayang.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecordValuesTest {

    @Test
    void trimsNullableTextToEmptyString() {
        assertThat(RecordValues.text(null)).isEmpty();
        assertThat(RecordValues.text("  value  ")).isEqualTo("value");
    }

    @Test
    void defaultsBlankValuesAndTrimsFallbacks() {
        assertThat(RecordValues.textOrDefault(null, " fallback ")).isEqualTo("fallback");
        assertThat(RecordValues.textOrDefault("   ", " fallback ")).isEqualTo("fallback");
        assertThat(RecordValues.textOrDefault(" value ", " fallback ")).isEqualTo("value");
    }

    @Test
    void treatsBlankFallbackAsEmptyString() {
        assertThat(RecordValues.textOrDefault(" ", null)).isEmpty();
        assertThat(RecordValues.textOrDefault(" ", "  ")).isEmpty();
    }
}

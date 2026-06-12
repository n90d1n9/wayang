package tech.kayys.wayang.a2ui.wayang.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecodeValuesTest {

    @Test
    void normalizesTextWithFallbacks() {
        assertThat(DecodeValues.rawText(" value ")).isEqualTo(" value ");
        assertThat(DecodeValues.rawText(null)).isEmpty();
        assertThat(DecodeValues.text(" value ")).isEqualTo("value");
        assertThat(DecodeValues.text(null)).isEmpty();
        assertThat(DecodeValues.text(" ", "fallback")).isEqualTo("fallback");
        assertThat(DecodeValues.text(null, "fallback")).isEqualTo("fallback");
    }

    @Test
    void parsesBooleansWithBlankFallbacks() {
        assertThat(DecodeValues.bool(true, false)).isTrue();
        assertThat(DecodeValues.bool(" true ", false)).isTrue();
        assertThat(DecodeValues.bool(" ", true)).isTrue();
        assertThat(DecodeValues.bool(null, false)).isFalse();
    }

    @Test
    void parsesNonNegativeIntegersWithFallbacks() {
        assertThat(DecodeValues.nonNegativeInt(3L, 0)).isEqualTo(3);
        assertThat(DecodeValues.nonNegativeInt(" 7 ", 0)).isEqualTo(7);
        assertThat(DecodeValues.nonNegativeInt("-2", 5)).isZero();
        assertThat(DecodeValues.nonNegativeInt("nan", 5)).isEqualTo(5);
        assertThat(DecodeValues.nonNegativeInt(null, 5)).isEqualTo(5);
    }

    @Test
    void parsesLenientLongsAndClampedIntegers() {
        assertThat(DecodeValues.nonNegativeLong(9, 0)).isEqualTo(9L);
        assertThat(DecodeValues.nonNegativeLong(" 11 ", 0)).isEqualTo(11L);
        assertThat(DecodeValues.nonNegativeLong("-3", 7)).isZero();
        assertThat(DecodeValues.nonNegativeLong("bad", 7)).isEqualTo(7L);
        assertThat(DecodeValues.clampedNonNegativeInt(Long.MAX_VALUE, 0))
                .isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void parsesNullableIntegersWithoutThrowing() {
        assertThat(DecodeValues.integerOrNull(200L)).isEqualTo(200);
        assertThat(DecodeValues.integerOrNull(" 404 ")).isEqualTo(404);
        assertThat(DecodeValues.integerOrNull("bad")).isNull();
        assertThat(DecodeValues.integerOrNull(" ")).isNull();
        assertThat(DecodeValues.integerOrNull(null)).isNull();
    }
}

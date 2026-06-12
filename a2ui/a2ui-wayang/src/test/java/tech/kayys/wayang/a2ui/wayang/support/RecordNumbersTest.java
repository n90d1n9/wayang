package tech.kayys.wayang.a2ui.wayang.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecordNumbersTest {

    @Test
    void clampsNegativeIntegersAndLongsToZero() {
        assertThat(RecordNumbers.nonNegative(-1)).isZero();
        assertThat(RecordNumbers.nonNegative(7)).isEqualTo(7);
        assertThat(RecordNumbers.nonNegative(-1L)).isZero();
        assertThat(RecordNumbers.nonNegative(9L)).isEqualTo(9L);
    }

    @Test
    void preservesNullNullableThresholds() {
        assertThat(RecordNumbers.nullableNonNegative((Integer) null)).isNull();
        assertThat(RecordNumbers.nullableNonNegative((Long) null)).isNull();
        assertThat(RecordNumbers.nullableNonNegative(-3)).isZero();
        assertThat(RecordNumbers.nullableNonNegative(-4L)).isZero();
    }

    @Test
    void clampsOneBasedIndexes() {
        assertThat(RecordNumbers.oneBased(-10)).isEqualTo(1);
        assertThat(RecordNumbers.oneBased(0)).isEqualTo(1);
        assertThat(RecordNumbers.oneBased(4)).isEqualTo(4);
    }
}

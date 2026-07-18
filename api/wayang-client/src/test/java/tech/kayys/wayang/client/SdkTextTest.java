package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.client.SdkText;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for shared SDK text normalization helpers.
 */
class SdkTextTest {

    @Test
    void blankToNullTrimsTextAndCollapsesMissingValues() {
        assertThat(SdkText.blankToNull(null)).isNull();
        assertThat(SdkText.blankToNull("   ")).isNull();
        assertThat(SdkText.blankToNull(" Wayang ")).isEqualTo("Wayang");
    }

    @Test
    void trimToDefaultUsesDefaultOnlyForBlankInput() {
        assertThat(SdkText.trimToDefault(null, "fallback")).isEqualTo("fallback");
        assertThat(SdkText.trimToDefault("  ", "fallback")).isEqualTo("fallback");
        assertThat(SdkText.trimToDefault(" Gollek ", "fallback")).isEqualTo("Gollek");
    }
}

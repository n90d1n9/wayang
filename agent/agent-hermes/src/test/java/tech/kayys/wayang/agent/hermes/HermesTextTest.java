package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HermesTextTest {

    @Test
    void oneLineOrReturnsFallbackForBlankValues() {
        assertThat(HermesText.oneLineOr(null, "fallback")).isEqualTo("fallback");
        assertThat(HermesText.oneLineOr("   ", "fallback")).isEqualTo("fallback");
    }

    @Test
    void oneLineOrCollapsesNonBlankValues() {
        assertThat(HermesText.oneLineOr(" alpha\n  beta\tgamma ", "fallback"))
                .isEqualTo("alpha beta gamma");
    }

    @Test
    void trimToEmptyReturnsEmptyForNullAndTrimsValues() {
        assertThat(HermesText.trimToEmpty(null)).isEmpty();
        assertThat(HermesText.trimToEmpty(" alpha\n  beta\tgamma "))
                .isEqualTo("alpha\n  beta\tgamma");
    }

    @Test
    void trimOrReturnsFallbackForBlankValues() {
        assertThat(HermesText.trimOr(null, "fallback")).isEqualTo("fallback");
        assertThat(HermesText.trimOr("   ", "fallback")).isEqualTo("fallback");
    }

    @Test
    void trimOrKeepsInternalWhitespace() {
        assertThat(HermesText.trimOr(" alpha\n  beta\tgamma ", "fallback"))
                .isEqualTo("alpha\n  beta\tgamma");
    }

    @Test
    void trimmedListDropsBlankValuesAndPreservesDuplicates() {
        assertThat(HermesText.trimmedList(List.of(" alpha ", " ", "beta", "alpha ")))
                .containsExactly("alpha", "beta", "alpha");
    }

    @Test
    void distinctTrimmedListDropsBlankValuesAndDeduplicates() {
        assertThat(HermesText.distinctTrimmedList(List.of(" alpha ", " ", "beta", "alpha ")))
                .containsExactly("alpha", "beta");
    }

    @Test
    void distinctOneLineListDropsBlankValuesCollapsesWhitespaceAndDeduplicates() {
        assertThat(HermesText.distinctOneLineList(Arrays.asList(" alpha\n beta ", null, " ", "alpha beta", "gamma")))
                .containsExactly("alpha beta", "gamma");
    }
}

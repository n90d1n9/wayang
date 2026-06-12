package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesCollectionsTest {

    @Test
    void copyNonNullReturnsEmptyListForNullValues() {
        assertThat(HermesCollections.copyNonNull(null)).isEmpty();
    }

    @Test
    void copyNonNullDropsNullsPreservesOrderAndCopiesDefensively() {
        List<String> values = Arrays.asList("first", null, "second");

        List<String> copy = HermesCollections.copyNonNull(values);

        assertThat(copy).containsExactly("first", "second");
        assertThatThrownBy(() -> copy.add("third"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

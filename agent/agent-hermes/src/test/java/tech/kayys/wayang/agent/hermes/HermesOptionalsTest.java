package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesOptionalsTest {

    @Test
    void convertsNullOptionalToEmpty() {
        assertThat(HermesOptionals.orEmpty(null)).isEmpty();
    }

    @Test
    void keepsExistingOptionalInstance() {
        Optional<String> value = Optional.of("hermes");

        assertThat(HermesOptionals.orEmpty(value)).isSameAs(value);
    }
}

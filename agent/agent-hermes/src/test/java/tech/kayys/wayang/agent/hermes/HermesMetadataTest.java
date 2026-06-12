package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesMetadataTest {

    @Test
    void copyReturnsEmptyMapForNullMetadata() {
        assertThat(HermesMetadata.copy(null)).isEmpty();
    }

    @Test
    void copyDefensivelyCopiesMetadata() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("status", "ready");

        Map<String, Object> copy = HermesMetadata.copy(source);
        source.put("status", "changed");

        assertThat(copy).containsEntry("status", "ready");
        assertThatThrownBy(() -> copy.put("extra", "ignored"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

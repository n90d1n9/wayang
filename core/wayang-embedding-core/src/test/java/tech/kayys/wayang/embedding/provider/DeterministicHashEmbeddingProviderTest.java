package tech.kayys.wayang.embedding.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DeterministicHashEmbeddingProviderTest {

    private final DeterministicHashEmbeddingProvider provider = new DeterministicHashEmbeddingProvider();

    @Test
    void shouldBeDeterministicForSameInput() {
        float[] v1 = provider.embedAll(List.of("Wayang embedding module"), "hash-384").get(0);
        float[] v2 = provider.embedAll(List.of("Wayang embedding module"), "hash-384").get(0);

        assertArrayEquals(v1, v2);
    }

    @Test
    void shouldUseDimensionFromModel() {
        float[] vector = provider.embedAll(List.of("hello world"), "hash-768").get(0);
        assertEquals(768, vector.length);
    }

    @Test
    void shouldDifferentiateDifferentInput() {
        float[] v1 = provider.embedAll(List.of("hello world"), "hash-384").get(0);
        float[] v2 = provider.embedAll(List.of("payment fraud detection"), "hash-384").get(0);

        assertFalse(java.util.Arrays.equals(v1, v2));
    }
}

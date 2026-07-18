package tech.kayys.wayang.embedding.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CharNgramEmbeddingProviderTest {

    private final CharNgramEmbeddingProvider provider = new CharNgramEmbeddingProvider();

    @Test
    void shouldBeDeterministicForSameInput() {
        float[] v1 = provider.embedAll(List.of("Wayang platform"), "chargram-512").get(0);
        float[] v2 = provider.embedAll(List.of("Wayang platform"), "chargram-512").get(0);
        assertArrayEquals(v1, v2);
    }

    @Test
    void shouldUseConfiguredDimension() {
        float[] vector = provider.embedAll(List.of("search indexing"), "chargram-300").get(0);
        assertEquals(300, vector.length);
    }

    @Test
    void shouldDifferentiateDifferentInputs() {
        float[] a = provider.embedAll(List.of("neural retrieval"), "chargram-512").get(0);
        float[] b = provider.embedAll(List.of("access control"), "chargram-512").get(0);
        assertFalse(java.util.Arrays.equals(a, b));
    }
}

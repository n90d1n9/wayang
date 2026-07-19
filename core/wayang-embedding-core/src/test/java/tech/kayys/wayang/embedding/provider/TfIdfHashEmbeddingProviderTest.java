package tech.kayys.wayang.embedding.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TfIdfHashEmbeddingProviderTest {

    private final TfIdfHashEmbeddingProvider provider = new TfIdfHashEmbeddingProvider();

    @Test
    void shouldSupportConfiguredDimensions() {
        float[] vector = provider.embedAll(List.of("payment fraud detection"), "tfidf-640").get(0);
        assertEquals(640, vector.length);
    }

    @Test
    void shouldGenerateDifferentVectorsForDifferentInputs() {
        float[] a = provider.embedAll(List.of("payment failed card declined"), "tfidf-384").get(0);
        float[] b = provider.embedAll(List.of("inventory stock replenishment"), "tfidf-384").get(0);

        assertFalse(java.util.Arrays.equals(a, b));
    }
}

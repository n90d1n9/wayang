package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RetrievalConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RagRuntimeConfigsTest {

    @Test
    void defaultsMissingRuntimeConfigs() {
        assertEquals(
                RetrievalConfig.defaults().topK(),
                RagRuntimeConfigs.retrievalOrDefault(null).topK());
        assertEquals(
                GenerationConfig.defaults().model(),
                RagRuntimeConfigs.generationOrDefault(null).model());
    }

    @Test
    void preservesProvidedRuntimeConfigs() {
        RetrievalConfig retrievalConfig = RetrievalConfig.defaults();
        GenerationConfig generationConfig = GenerationConfig.defaults();

        assertSame(retrievalConfig, RagRuntimeConfigs.retrievalOrDefault(retrievalConfig));
        assertSame(generationConfig, RagRuntimeConfigs.generationOrDefault(generationConfig));
    }
}

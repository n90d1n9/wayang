package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.*;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeGenerationServiceTest {

    private final NativeGenerationService service = new NativeGenerationService();

    @Test
    void shouldGenerateContextModeByDefault() {
        String answer = service.generate(
                RagQuery.of("What is this?"),
                List.of(new RagScoredChunk(RagChunk.of("d1", 0, "Context A", Map.of()), 0.9)),
                GenerationConfig.defaults());

        assertTrue(answer.contains("Q: What is this?"));
        assertTrue(answer.contains("Context A"));
    }

    @Test
    void shouldGenerateExtractiveMode() {
        GenerationConfig extractive = new GenerationConfig(
                "native-extractive",
                "n/a",
                0.0f,
                256,
                1.0f,
                0.0f,
                0.0f,
                List.of(),
                "",
                null,
                true,
                true,
                tech.kayys.wayang.rag.core.CitationStyle.INLINE_NUMBERED,
                false,
                false,
                Map.of());

        String answer = service.generate(
                RagQuery.of("Explain"),
                List.of(new RagScoredChunk(RagChunk.of("d1", 0, "First sentence. Second sentence.", Map.of()), 0.9)),
                extractive);

        assertTrue(answer.contains("First sentence."));
        assertTrue(answer.contains("[source:"));
    }
}

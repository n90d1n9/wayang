package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationConfigTest {

    @Test
    void copiesListAndMetadataInputsDefensively() {
        List<String> stopSequences = new ArrayList<>(List.of("STOP"));
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("mode", "extractive");
        additionalParams.put("nullable", null);
        Map<String, Object> safetySettings = new HashMap<>();
        safetySettings.put("policy", "strict");

        GenerationConfig config = new GenerationConfig(
                "provider",
                "model",
                0.2f,
                512,
                0.9f,
                0.1f,
                0.2f,
                stopSequences,
                "system",
                additionalParams,
                true,
                true,
                CitationStyle.FOOTNOTE,
                true,
                true,
                safetySettings);
        stopSequences.add("MUTATED");
        additionalParams.put("mode", "mutated");
        safetySettings.put("policy", "mutated");

        assertEquals(List.of("STOP"), config.stopSequences());
        assertEquals("extractive", config.additionalParams().get("mode"));
        assertTrue(config.additionalParams().containsKey("nullable"));
        assertNull(config.additionalParams().get("nullable"));
        assertEquals("strict", config.safetySettings().get("policy"));
        assertThrows(UnsupportedOperationException.class, () -> config.stopSequences().add("other"));
        assertThrows(UnsupportedOperationException.class, () -> config.additionalParams().put("other", "value"));
        assertThrows(UnsupportedOperationException.class, () -> config.safetySettings().put("other", "value"));
    }

    @Test
    void defaultsMissingCollectionsToEmptyImmutableValues() {
        GenerationConfig config = new GenerationConfig(
                "provider",
                "model",
                0.2f,
                512,
                0.9f,
                0.1f,
                0.2f,
                null,
                "system",
                null,
                true,
                true,
                CitationStyle.FOOTNOTE,
                true,
                true,
                null);

        assertEquals(List.of(), config.stopSequences());
        assertEquals(Map.of(), config.additionalParams());
        assertEquals(Map.of(), config.safetySettings());
        assertThrows(UnsupportedOperationException.class, () -> config.stopSequences().add("other"));
        assertThrows(UnsupportedOperationException.class, () -> config.additionalParams().put("other", "value"));
        assertThrows(UnsupportedOperationException.class, () -> config.safetySettings().put("other", "value"));
    }
}

package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagResultTest {

    @Test
    void copiesMetadataDefensivelyAndPreservesNullableValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "native");
        metadata.put("nullable", null);

        RagResult result = new RagResult(RagQuery.of("question"), List.of(), "answer", metadata);
        metadata.put("provider", "mutated");

        assertEquals("native", result.metadata().get("provider"));
        assertTrue(result.metadata().containsKey("nullable"));
        assertNull(result.metadata().get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> result.metadata().put("other", "value"));
    }
}

package tech.kayys.wayang.rag.plugin.api;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagPluginMetadataTest {

    @Test
    void copiesMetadataDefensivelyAndPreservesNullableValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "plugin");
        metadata.put("nullable", null);

        Map<String, Object> copied = RagPluginMetadata.copy(metadata);
        metadata.put("source", "mutated");

        assertEquals("plugin", copied.get("source"));
        assertTrue(copied.containsKey("nullable"));
        assertNull(copied.get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> copied.put("other", "value"));
    }

    @Test
    void metadataWithSingleValuePreservesNullableBaseValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("nullable", null);

        Map<String, Object> updated = RagPluginSupport.metadataWith(metadata, "annotated", true);

        assertTrue(updated.containsKey("nullable"));
        assertNull(updated.get("nullable"));
        assertEquals(true, updated.get("annotated"));
        assertThrows(UnsupportedOperationException.class, () -> updated.put("other", "value"));
    }

    @Test
    void metadataWithAdditionsPreservesNullableAdditionValues() {
        Map<String, Object> additions = new HashMap<>();
        additions.put("nullable", null);
        additions.put("score", 0.8);

        Map<String, Object> updated = RagPluginSupport.metadataWith(Map.of("source", "chunk"), additions);
        additions.put("score", 0.1);

        assertEquals("chunk", updated.get("source"));
        assertEquals(0.8, updated.get("score"));
        assertTrue(updated.containsKey("nullable"));
        assertNull(updated.get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> updated.put("other", "value"));
    }
}

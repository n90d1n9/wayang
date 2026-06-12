package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentSourceTest {

    @Test
    void copiesMetadataDefensivelyAndSkipsNullEntries() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("collection", "docs");
        metadata.put("ignored", null);
        metadata.put(null, "ignored");

        DocumentSource source = new DocumentSource(
                SourceType.TEXT,
                "note.txt",
                "content",
                metadata);
        metadata.put("collection", "mutated");

        assertEquals(Map.of("collection", "docs"), source.metadata());
        assertFalse(source.metadata().containsKey("ignored"));
        assertThrows(UnsupportedOperationException.class, () -> source.metadata().put("other", "value"));
    }

    @Test
    void defaultsNullMetadataToEmptyMap() {
        DocumentSource source = new DocumentSource(SourceType.TEXT, null, "content", null);

        assertEquals(Map.of(), source.metadata());
    }
}

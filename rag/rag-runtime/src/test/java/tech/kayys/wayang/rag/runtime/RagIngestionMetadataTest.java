package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.RagMetadataKeys;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagIngestionMetadataTest {

    @Test
    void buildsPdfMetadataWithTenantFilenameAndDefaultCollection() {
        Map<String, Object> metadata = RagIngestionMetadata.pdf(
                "tenant",
                Path.of("/tmp/manual.pdf"),
                Map.of(RagMetadataKeys.COLLECTION, " "));

        assertEquals("tenant", metadata.get(RagMetadataKeys.TENANT_ID));
        assertEquals("manual.pdf", metadata.get(RagMetadataKeys.SOURCE));
        assertEquals(RagRuntimeDefaults.DEFAULT_COLLECTION, metadata.get(RagMetadataKeys.COLLECTION));
    }

    @Test
    void callerMetadataCanOverridePdfSourceBeforeDocumentCreation() {
        Map<String, Object> metadata = RagIngestionMetadata.pdf(
                "tenant",
                Path.of("/tmp/manual.pdf"),
                Map.of(RagMetadataKeys.SOURCE, "uploaded-name.pdf", RagMetadataKeys.COLLECTION, " docs "));

        assertEquals("uploaded-name.pdf", RagIngestionMetadata.source(metadata));
        assertEquals("docs", metadata.get(RagMetadataKeys.COLLECTION));
    }

    @Test
    void textMetadataKeepsOptionalSourceAndSkipsNullValues() {
        Map<String, String> input = new HashMap<>();
        input.put(RagMetadataKeys.SOURCE, "note.txt");
        input.put("ignored", null);

        Map<String, Object> metadata = RagIngestionMetadata.text("tenant", input);

        assertEquals("tenant", metadata.get(RagMetadataKeys.TENANT_ID));
        assertEquals("note.txt", RagIngestionMetadata.source(metadata));
        assertFalse(metadata.containsKey("ignored"));
    }

    @Test
    void copiesMetadataDefensively() {
        Map<String, Object> input = new HashMap<>();
        input.put("key", "value");

        Map<String, Object> metadata = RagIngestionMetadata.copy(input);
        input.put("key", "changed");

        assertEquals("value", metadata.get("key"));
        assertThrows(UnsupportedOperationException.class, () -> metadata.put("other", "value"));
    }
}

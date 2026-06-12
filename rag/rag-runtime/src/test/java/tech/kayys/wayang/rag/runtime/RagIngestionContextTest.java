package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.RagMetadataKeys;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagIngestionContextTest {

    @Test
    void pdfDocumentsNormalizeBlankCollectionMetadataToDefault() {
        RagIngestionDocument document = RagIngestionDocument.pdf(
                "tenant",
                Path.of("/tmp/doc.pdf"),
                "content",
                Map.of(RagMetadataKeys.COLLECTION, " "));

        assertEquals(
                RagRuntimeDefaults.DEFAULT_COLLECTION,
                document.metadata().get(RagMetadataKeys.COLLECTION));
        assertEquals("doc.pdf", document.source());
    }

    @Test
    void pdfDocumentsTrimConfiguredCollectionMetadata() {
        RagIngestionDocument document = RagIngestionDocument.pdf(
                "tenant",
                Path.of("/tmp/doc.pdf"),
                "content",
                Map.of(RagMetadataKeys.COLLECTION, " docs "));

        assertEquals("docs", document.metadata().get(RagMetadataKeys.COLLECTION));
    }
}

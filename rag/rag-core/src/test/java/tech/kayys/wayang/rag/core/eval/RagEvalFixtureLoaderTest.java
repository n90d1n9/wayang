package tech.kayys.wayang.rag.core.eval;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagEvalFixtureLoaderTest {

    @Test
    void shouldLoadTsvFixture() throws IOException {
        Path file = Files.createTempFile("rag-eval", ".tsv");
        Files.writeString(file, """
                # query\trelevant_ids
                how to retry payments?\tchunk-1,chunk-2
                fraud rules\tchunk-9
                """);

        List<RagEvalCase> cases = RagEvalFixtureLoader.loadTsv(file);

        assertEquals(2, cases.size());
        assertEquals("how to retry payments?", cases.get(0).query());
        assertEquals(List.of("chunk-1", "chunk-2"), cases.get(0).relevantChunkIds());
        assertEquals("fraud rules", cases.get(1).query());
        assertEquals(List.of("chunk-9"), cases.get(1).relevantChunkIds());
    }
}

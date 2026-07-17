package tech.kayys.wayang.rag.core.spi;

import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.List;

import io.smallrye.mutiny.Multi;

public interface Generator {
    String generate(RagQuery query, List<RagScoredChunk> context);

    default Multi<String> generateStream(RagQuery query, List<RagScoredChunk> context) {
        return Multi.createFrom().item(generate(query, context));
    }
}

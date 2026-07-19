package tech.kayys.wayang.rag.core.impl;

import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.spi.Generator;

import java.util.List;

public class ContextConcatenatingGenerator implements Generator {

    @Override
    public String generate(RagQuery query, List<RagScoredChunk> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Q: ").append(query.text()).append("\n");
        sb.append("Context:\n");
        for (RagScoredChunk scored : context) {
            sb.append("- ").append(scored.chunk().text()).append("\n");
        }
        return sb.toString().trim();
    }
}

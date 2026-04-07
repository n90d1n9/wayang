package tech.kayys.wayang.rag.retrieval;

import java.util.Map;
import tech.kayys.wayang.rag.core.RagEvalDataset;

public record RagRetrievalEvalRequest(
                String tenantId,
                Integer topK,
                Double minSimilarity,
                String matchField,
                Map<String, Object> filters,
                String fixturePath,
                RagEvalDataset dataset) {
}

package tech.kayys.wayang.rag.core;

import java.util.List;
import java.util.Map;

public record RetrievalResult(
                int resultsRetrieved, int finalResults,
                double avgScore, double maxScore, double minScore,
                List<String> contexts, List<Map<String, Object>> metadata,
                boolean reranked) {
}
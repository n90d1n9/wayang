package tech.kayys.wayang.rag.core.eval;

import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.spi.Retriever;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RagEvaluationHarness {

    public RagEvalResult evaluate(Retriever retriever, List<RagEvalCase> evalCases, int topK) {
        if (retriever == null) {
            throw new IllegalArgumentException("retriever must not be null");
        }
        if (evalCases == null || evalCases.isEmpty()) {
            throw new IllegalArgumentException("evalCases must not be empty");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be > 0");
        }

        double recallSum = 0.0;
        double mrrSum = 0.0;
        List<Long> latenciesMs = new ArrayList<>(evalCases.size());

        for (RagEvalCase evalCase : evalCases) {
            long started = System.nanoTime();
            List<RagScoredChunk> results = retriever
                    .retrieve(new RagQuery(evalCase.query(), topK, 0.0, java.util.Map.of()));
            long elapsedMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
            latenciesMs.add(elapsedMs);

            Set<String> relevant = new HashSet<>(evalCase.relevantChunkIds());
            int hits = 0;
            double reciprocalRank = 0.0;
            int rank = 0;
            for (RagScoredChunk result : results) {
                rank++;
                String id = result.chunk() == null ? null : result.chunk().id();
                if (id != null && relevant.contains(id)) {
                    hits++;
                    if (reciprocalRank == 0.0) {
                        reciprocalRank = 1.0 / rank;
                    }
                }
            }

            recallSum += ((double) hits / relevant.size());
            mrrSum += reciprocalRank;
        }

        latenciesMs.sort(Comparator.naturalOrder());
        long p95 = percentile(latenciesMs, 0.95);
        long avg = (long) latenciesMs.stream().mapToLong(Long::longValue).average().orElse(0.0);
        int total = evalCases.size();
        return new RagEvalResult(
                total,
                topK,
                recallSum / total,
                mrrSum / total,
                p95,
                avg);
    }

    private static long percentile(List<Long> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0L;
        }
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        int safeIndex = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(safeIndex);
    }
}

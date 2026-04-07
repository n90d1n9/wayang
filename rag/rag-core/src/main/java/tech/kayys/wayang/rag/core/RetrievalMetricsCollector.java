package tech.kayys.wayang.rag.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RETRIEVAL METRICS COLLECTOR
 */
@ApplicationScoped
class RetrievalMetricsCollector {

        private static final Logger LOG = LoggerFactory.getLogger(RetrievalMetricsCollector.class);

        private final Map<String, RetrievalMetrics> metrics = new ConcurrentHashMap<>();

        public void recordRetrieval(
                        String workflowRunId,
                        int resultsRetrieved,
                        int finalResults,
                        long durationMs,
                        double avgScore) {

                LOG.info("Retrieval metrics - Run: {}, Retrieved: {}, Final: {}, AvgScore: {:.3f}, Duration: {}ms",
                                workflowRunId, resultsRetrieved, finalResults, avgScore, durationMs);

                metrics.put(workflowRunId, new RetrievalMetrics(
                                resultsRetrieved, finalResults, avgScore, durationMs, Instant.now()));
        }

        public Optional<RetrievalMetrics> getMetrics(String workflowRunId) {
                return Optional.ofNullable(metrics.get(workflowRunId));
        }

        record RetrievalMetrics(
                        int resultsRetrieved,
                        int finalResults,
                        double avgScore,
                        long durationMs,
                        Instant timestamp) {
        }
}

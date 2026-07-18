package tech.kayys.wayang.rag.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class GenerationMetricsCollector {

    private static final Logger LOG = LoggerFactory.getLogger(GenerationMetricsCollector.class);

    public void recordGeneration(String workflowRunId, int tokensUsed, long durationMs) {
        LOG.info("Generation metrics - Run: {}, Tokens: {}, Duration: {}ms",
                workflowRunId, tokensUsed, durationMs);
    }
}
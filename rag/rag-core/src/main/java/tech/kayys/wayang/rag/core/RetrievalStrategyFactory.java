package tech.kayys.wayang.rag.core;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.embedding.EmbeddingService;

/**
 * RETRIEVAL STRATEGY FACTORY - INTERNAL IMPLEMENTATION
 */
@ApplicationScoped
public class RetrievalStrategyFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RetrievalStrategyFactory.class);

    @Inject
    EmbeddingService embeddingService;

    public RetrievalStrategy getStrategy(String strategyType) {
        LOG.debug("Creating retrieval strategy: {}", strategyType);

        return switch (strategyType.toLowerCase()) {
            case "dense" -> new DenseRetrievalStrategy(embeddingService);
            case "hybrid" -> new HybridRetrievalStrategy(embeddingService);
            case "keyword" -> new KeywordRetrievalStrategy();
            default -> {
                LOG.warn("Unknown strategy: {}, using dense", strategyType);
                yield new DenseRetrievalStrategy(embeddingService);
            }
        };
    }
}

package tech.kayys.wayang.memory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Startup initializer
 */
@ApplicationScoped
public class MemoryExecutorInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryExecutorInitializer.class);

    @Inject
    VectorStoreFactory vectorStoreFactory;

    @Inject
    EmbeddingServiceFactory embeddingServiceFactory;

    void onStart(@jakarta.enterprise.event.Observes io.quarkus.runtime.StartupEvent event) {
        LOG.info("Initializing Memory Executor components...");

        // Initialize vector store
        VectorMemoryStore vectorStore = vectorStoreFactory.getVectorStore();
        LOG.info("Vector store initialized: {}", vectorStore.getClass().getSimpleName());

        // Initialize embedding service
        EmbeddingService embeddingService = embeddingServiceFactory.getEmbeddingService();
        LOG.info("Embedding service initialized: {} (dimension: {})",
                embeddingService.getProvider(),
                embeddingService.getDimension());

        LOG.info("Memory Executor initialization complete");
    }
}
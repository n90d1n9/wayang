package tech.kayys.wayang.embedding;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingModuleConfigTest {

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("wayang.embedding.default-provider");
        System.clearProperty("wayang.embedding.default-model");
        System.clearProperty("wayang.embedding.normalize");
        System.clearProperty("wayang.embedding.tenant-strategies");
        System.clearProperty("wayang.embedding.version");
        System.clearProperty("wayang.embedding.cache.enabled");
        System.clearProperty("wayang.embedding.cache.max-entries");
        System.clearProperty("wayang.embedding.batch.size");
        System.clearProperty("wayang.embedding.batch.queue-capacity");
        System.clearProperty("wayang.embedding.batch.max-retries");
        System.clearProperty("wayang.embedding.batch.worker-threads");
    }

    @Test
    void shouldLoadTenantStrategiesFromSpec() {
        EmbeddingModuleConfig config = new EmbeddingModuleConfig();
        config.loadTenantStrategies("tenant-a=tfidf:tfidf-512;tenant-b|hash|hash-384");

        var a = config.tenantStrategies().find("tenant-a");
        var b = config.tenantStrategies().find("tenant-b");

        assertTrue(a.isPresent());
        assertEquals("tfidf", a.get().provider());
        assertEquals("tfidf-512", a.get().model());

        assertTrue(b.isPresent());
        assertEquals("hash", b.get().provider());
        assertEquals("hash-384", b.get().model());
    }

    @Test
    void shouldRejectInvalidStrategyFormat() {
        EmbeddingModuleConfig config = new EmbeddingModuleConfig();
        assertThrows(IllegalArgumentException.class, () -> config.loadTenantStrategies("tenant-a=tfidf"));
    }

    @Test
    void shouldApplyOverridesFromSystemProperties() {
        System.setProperty("wayang.embedding.default-provider", "chargram");
        System.setProperty("wayang.embedding.default-model", "chargram-256");
        System.setProperty("wayang.embedding.normalize", "false");
        System.setProperty("wayang.embedding.tenant-strategies", "tenant-x=hash:hash-768");
        System.setProperty("wayang.embedding.version", "v2");
        System.setProperty("wayang.embedding.cache.enabled", "false");
        System.setProperty("wayang.embedding.cache.max-entries", "123");
        System.setProperty("wayang.embedding.batch.size", "7");
        System.setProperty("wayang.embedding.batch.queue-capacity", "33");
        System.setProperty("wayang.embedding.batch.max-retries", "5");
        System.setProperty("wayang.embedding.batch.worker-threads", "4");

        EmbeddingModuleConfig config = new EmbeddingModuleConfig();

        assertEquals("chargram", config.getDefaultProvider());
        assertEquals("chargram-256", config.getDefaultModel());
        assertTrue(!config.isNormalize());
        assertEquals("v2", config.getEmbeddingVersion());
        assertTrue(!config.isCacheEnabled());
        assertEquals(123, config.getCacheMaxEntries());
        assertEquals(7, config.getBatchSize());
        assertEquals(33, config.getBatchQueueCapacity());
        assertEquals(5, config.getBatchMaxRetries());
        assertEquals(4, config.getBatchWorkerThreads());
        var strategy = config.tenantStrategies().find("tenant-x");
        assertTrue(strategy.isPresent());
        assertEquals("hash", strategy.get().provider());
        assertEquals("hash-768", strategy.get().model());
    }
}

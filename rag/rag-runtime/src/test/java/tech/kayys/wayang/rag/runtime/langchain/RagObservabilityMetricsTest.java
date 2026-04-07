package tech.kayys.wayang.rag.runtime.langchain;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import tech.kayys.wayang.rag.runtime.RagObservabilityMetrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RagObservabilityMetricsTest {

    @Test
    void shouldRecordEmbeddingSearchAndIngestionMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RagObservabilityMetrics metrics = new RagObservabilityMetrics(registry);
        metrics.initGauge();

        metrics.recordEmbeddingSuccess("hash-384", 3, 12);
        metrics.recordEmbeddingFailure("hash-384");
        metrics.recordSearchSuccess("tenant-a", 20, 4);
        metrics.recordSearchFailure("tenant-a");
        metrics.recordIngestion("tenant-a", 2, 5, 37);

        assertNotNull(registry.find("wayang.rag.embedding.latency").timer());
        assertNotNull(registry.find("wayang.rag.search.latency").timer());
        assertNotNull(registry.find("wayang.rag.ingest.latency").timer());
        assertEquals(1.0, registry.find("wayang.rag.embedding.failure.count").counter().count());
        assertEquals(1.0, registry.find("wayang.rag.search.failure.count").counter().count());
        assertEquals(37L, metrics.currentIndexLagMs());
        assertEquals(37.0, registry.find("wayang.rag.index.lag.ms").gauge().value());
    }
}

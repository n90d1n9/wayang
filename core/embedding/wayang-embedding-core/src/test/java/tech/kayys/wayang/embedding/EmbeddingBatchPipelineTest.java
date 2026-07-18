package tech.kayys.wayang.embedding;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.embedding.provider.DeterministicHashEmbeddingProvider;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingBatchPipelineTest {

    @Test
    void shouldProcessRequestsInBatches() {
        EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry(List.of(new DeterministicHashEmbeddingProvider()));
        EmbeddingModuleConfig config = new EmbeddingModuleConfig();
        config.setDefaultProvider("hash");
        config.setDefaultModel("hash-16");
        config.setBatchSize(2);
        config.setBatchQueueCapacity(8);
        config.setBatchWorkerThreads(1);

        EmbeddingService service = new EmbeddingService(registry, config);
        try (EmbeddingBatchPipeline pipeline = new EmbeddingBatchPipeline(service, config)) {
            EmbeddingResponse response = pipeline.submitAndWait(
                    "tenant-a",
                    new EmbeddingRequest(List.of("a", "b", "c", "d", "e"), null, null, false),
                    Duration.ofSeconds(3));

            assertEquals(5, response.embeddings().size());
            assertEquals(16, response.dimension());
        }
    }

    @Test
    void shouldRetryOnTransientFailure() {
        FlakyProvider flaky = new FlakyProvider();
        EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry(List.of(flaky));
        EmbeddingModuleConfig config = new EmbeddingModuleConfig();
        config.setDefaultProvider("flaky");
        config.setDefaultModel("flaky-2");
        config.setBatchSize(2);
        config.setBatchQueueCapacity(4);
        config.setBatchWorkerThreads(1);
        config.setBatchMaxRetries(2);

        EmbeddingService service = new EmbeddingService(registry, config);
        try (EmbeddingBatchPipeline pipeline = new EmbeddingBatchPipeline(service, config)) {
            EmbeddingResponse response = pipeline.submitAndWait(
                    "tenant-r",
                    new EmbeddingRequest(List.of("x", "y"), null, null, false),
                    Duration.ofSeconds(3));

            assertEquals(2, response.embeddings().size());
            assertEquals(2, response.dimension());
            assertEquals(2, flaky.calls.get());
        }
    }

    @Test
    void shouldApplyBackpressureWhenQueueFull() {
        SlowProvider slow = new SlowProvider();
        EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry(List.of(slow));
        EmbeddingModuleConfig config = new EmbeddingModuleConfig();
        config.setDefaultProvider("slow");
        config.setDefaultModel("slow-2");
        config.setBatchSize(1);
        config.setBatchQueueCapacity(1);
        config.setBatchWorkerThreads(1);

        EmbeddingService service = new EmbeddingService(registry, config);
        try (EmbeddingBatchPipeline pipeline = new EmbeddingBatchPipeline(service, config)) {
            pipeline.submit("tenant-a", EmbeddingRequest.single("a"));
            boolean rejected = false;
            for (int i = 0; i < 100; i++) {
                try {
                    pipeline.submit("tenant-a", EmbeddingRequest.single("b-" + i));
                } catch (EmbeddingException ex) {
                    rejected = true;
                    break;
                }
            }
            assertTrue(rejected);
        }
    }

    private static final class FlakyProvider implements EmbeddingProvider {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public String name() {
            return "flaky";
        }

        @Override
        public boolean supports(String model) {
            return "flaky-2".equals(model);
        }

        @Override
        public List<float[]> embedAll(List<String> inputs, String model) {
            int call = calls.incrementAndGet();
            if (call == 1) {
                throw new EmbeddingException("transient");
            }
            return inputs.stream().map(i -> new float[] { 1f, 2f }).toList();
        }
    }

    private static final class SlowProvider implements EmbeddingProvider {
        @Override
        public String name() {
            return "slow";
        }

        @Override
        public boolean supports(String model) {
            return "slow-2".equals(model);
        }

        @Override
        public List<float[]> embedAll(List<String> inputs, String model) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return inputs.stream().map(i -> new float[] { 1f, 0f }).toList();
        }
    }
}

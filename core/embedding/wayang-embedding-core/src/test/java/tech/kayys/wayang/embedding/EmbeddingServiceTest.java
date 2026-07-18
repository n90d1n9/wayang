package tech.kayys.wayang.embedding;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.embedding.provider.CharNgramEmbeddingProvider;
import tech.kayys.wayang.embedding.provider.DeterministicHashEmbeddingProvider;
import tech.kayys.wayang.embedding.provider.TfIdfHashEmbeddingProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingServiceTest {

    @Test
    void shouldUseDefaultsAndNormalize() {
        EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry(List.of(
                new DeterministicHashEmbeddingProvider(),
                new TfIdfHashEmbeddingProvider(),
                new CharNgramEmbeddingProvider()));
        EmbeddingModuleConfig config = new EmbeddingModuleConfig();
        config.setDefaultProvider("hash");
        config.setDefaultModel("hash-384");
        config.setNormalize(true);

        EmbeddingService service = new EmbeddingService(registry, config);
        EmbeddingResponse response = service.embed(EmbeddingRequest.single("what is wayang")).await().indefinitely();

        assertEquals("hash", response.provider());
        assertEquals("hash-384", response.model());
        assertEquals(384, response.dimension());
        assertEquals("v1", response.version());

        float[] vector = response.first();
        double sumSquares = 0.0;
        for (float value : vector) {
            sumSquares += value * value;
        }
        assertTrue(Math.abs(Math.sqrt(sumSquares) - 1.0) < 0.0001);
    }

    @Test
    void shouldAllowModelOverride() {
        EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry(List.of(
                new DeterministicHashEmbeddingProvider(),
                new TfIdfHashEmbeddingProvider(),
                new CharNgramEmbeddingProvider()));
        EmbeddingService service = new EmbeddingService(registry, new EmbeddingModuleConfig());

        EmbeddingRequest req = new EmbeddingRequest(List.of("hello"), "hash-1536", "hash", false);
        EmbeddingResponse response = service.embed(req).await().indefinitely();

        assertEquals(1536, response.first().length);
    }

    @Test
    void shouldAutoResolveProviderFromModel() {
        EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry(List.of(
                new DeterministicHashEmbeddingProvider(),
                new TfIdfHashEmbeddingProvider(),
                new CharNgramEmbeddingProvider()));
        EmbeddingModuleConfig config = new EmbeddingModuleConfig();
        config.setDefaultProvider("hash");
        config.setDefaultModel("hash-384");

        EmbeddingService service = new EmbeddingService(registry, config);
        EmbeddingResponse tfidf = service.embed(new EmbeddingRequest(List.of("risk scoring"), "tfidf-256", null, false))
                .await().indefinitely();
        EmbeddingResponse chargram = service
                .embed(new EmbeddingRequest(List.of("risk scoring"), "chargram-256", null, false)).await()
                .indefinitely();

        assertEquals("tfidf", tfidf.provider());
        assertEquals(256, tfidf.dimension());
        assertEquals("chargram", chargram.provider());
        assertEquals(256, chargram.dimension());
    }

    @Test
    void shouldResolveTenantStrategyWhenRequestDoesNotSpecifyProviderOrModel() {
        EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry(List.of(
                new DeterministicHashEmbeddingProvider(),
                new TfIdfHashEmbeddingProvider(),
                new CharNgramEmbeddingProvider()));
        EmbeddingModuleConfig config = new EmbeddingModuleConfig();
        config.setDefaultProvider("hash");
        config.setDefaultModel("hash-384");
        config.setTenantStrategy("tenant-risk", "tfidf", "tfidf-512");

        EmbeddingService service = new EmbeddingService(registry, config);
        EmbeddingResponse response = service.embedForTenant(
                "tenant-risk",
                new EmbeddingRequest(List.of("risk score card declined"), null, null, false)).await().indefinitely();

        assertEquals("tfidf", response.provider());
        assertEquals("tfidf-512", response.model());
        assertEquals(512, response.dimension());
    }

    @Test
    void shouldFailWhenProviderReturnsWrongModelDimension() {
        EmbeddingProvider broken = new EmbeddingProvider() {
            @Override
            public String name() {
                return "broken";
            }

            @Override
            public boolean supports(String model) {
                return "broken-8".equals(model);
            }

            @Override
            public List<float[]> embedAll(List<String> inputs, String model) {
                return List.of(new float[4]);
            }
        };

        EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry(List.of(broken));
        EmbeddingModuleConfig config = new EmbeddingModuleConfig();
        config.setDefaultProvider("broken");
        config.setDefaultModel("broken-8");
        EmbeddingService service = new EmbeddingService(registry, config);

        assertThrows(
                EmbeddingException.class,
                () -> service.embed(EmbeddingRequest.single("dimension mismatch")).await().indefinitely());
    }

    @Test
    void shouldDeduplicateAndCacheEmbeddingsByTenantModelAndText() {
        CountingProvider countingProvider = new CountingProvider();
        EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry(List.of(countingProvider));
        EmbeddingModuleConfig config = new EmbeddingModuleConfig();
        config.setDefaultProvider("counting");
        config.setDefaultModel("counting-3");
        config.setCacheEnabled(true);
        config.setCacheMaxEntries(100);

        EmbeddingService service = new EmbeddingService(registry, config);
        EmbeddingRequest request = new EmbeddingRequest(List.of("alpha", "beta", "alpha"), null, null, false);
        EmbeddingResponse first = service.embedForTenant("tenant-a", request).await().indefinitely();
        EmbeddingResponse second = service.embedForTenant("tenant-a", request).await().indefinitely();

        assertEquals(3, first.dimension());
        assertEquals(1, countingProvider.calls.get());
        assertEquals(2, countingProvider.totalInputs.get());
        assertEquals(first.embeddings().size(), second.embeddings().size());
    }

    @Test
    void shouldUseVersionFromConfig() {
        EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry(
                List.of(new DeterministicHashEmbeddingProvider()));
        EmbeddingModuleConfig config = new EmbeddingModuleConfig();
        config.setEmbeddingVersion("v7");
        config.setDefaultProvider("hash");
        config.setDefaultModel("hash");
        EmbeddingService service = new EmbeddingService(registry, config);

        EmbeddingResponse response = service.embed(EmbeddingRequest.single("hello")).await().indefinitely();

        assertEquals("v7", response.version());
    }

    private static final class CountingProvider implements EmbeddingProvider {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger totalInputs = new AtomicInteger();

        @Override
        public String name() {
            return "counting";
        }

        @Override
        public boolean supports(String model) {
            return "counting-3".equals(model);
        }

        @Override
        public List<float[]> embedAll(List<String> inputs, String model) {
            calls.incrementAndGet();
            totalInputs.addAndGet(inputs.size());
            return inputs.stream()
                    .map(text -> new float[] { text.length(), 1f, 0.5f })
                    .toList();
        }
    }
}

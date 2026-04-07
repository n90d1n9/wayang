package tech.kayys.wayang.rag.embedding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.wayang.embedding.EmbeddingService;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class RagModelFactoryTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private jakarta.enterprise.inject.Instance<EmbeddingMetrics> metricsInstance;

    private RagModelFactory modelFactory;

    @BeforeEach
    void setUp() {
        modelFactory = new RagModelFactory();
        modelFactory.embeddingService = embeddingService;
        modelFactory.metricsInstance = metricsInstance;
    }

    @Test
    void testCreateEmbeddingModel() {
        RagEmbeddingModel result = modelFactory.createEmbeddingModel("tenant", "hash-512");
        assertNotNull(result);
    }

    @Test
    void testCreateChatModel() {
        var result = modelFactory.createChatModel("tenant", "gpt-4");
        assertNull(result);
    }
}

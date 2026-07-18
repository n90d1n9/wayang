package tech.kayys.gamelan.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.embedding.EmbeddingRequest;
import tech.kayys.gollek.spi.embedding.EmbeddingResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock GollekSdk sdk;
    @InjectMocks EmbeddingService service;

    private EmbeddingResponse resp(float[] vec) {
        EmbeddingResponse r = mock(EmbeddingResponse.class);
        when(r.embeddings()).thenReturn(List.of(vec));
        return r;
    }

    @Test
    void embedReturnsNormalisedVector() throws SdkException {
        when(sdk.createEmbedding(any())).thenReturn(resp(new float[]{3f, 4f}));

        float[] result = service.embed("hello");

        assertThat(result).hasSize(2);
        // L2 norm of [3,4] = 5; normalised: [0.6, 0.8]
        assertThat(result[0]).isCloseTo(0.6f, org.assertj.core.data.Offset.offset(0.001f));
        assertThat(result[1]).isCloseTo(0.8f, org.assertj.core.data.Offset.offset(0.001f));
    }

    @Test
    void embedReturnsEmptyOnSdkException() throws SdkException {
        when(sdk.createEmbedding(any())).thenThrow(new SdkException("model unavailable"));
        assertThat(service.embed("hello")).isEmpty();
    }

    @Test
    void embedReturnsCachedResult() throws SdkException {
        when(sdk.createEmbedding(any())).thenReturn(resp(new float[]{1f, 0f}));

        service.embed("same text");
        service.embed("same text");

        // SDK called only once — second call is cache hit
        verify(sdk, times(1)).createEmbedding(any());
    }

    @Test
    void embedBlankTextReturnsEmpty() {
        assertThat(service.embed("")).isEmpty();
        assertThat(service.embed("   ")).isEmpty();
        assertThat(service.embed(null)).isEmpty();
    }

    @Test
    void clearCacheInvalidatesCache() throws SdkException {
        when(sdk.createEmbedding(any())).thenReturn(resp(new float[]{1f, 0f}));

        service.embed("text");
        service.clearCache();
        service.embed("text");

        verify(sdk, times(2)).createEmbedding(any());
    }

    @Test
    void l2NormaliseProducesUnitVector() {
        float[] v = {3f, 4f};
        float[] normalised = EmbeddingService.l2Normalise(v);
        double norm = Math.sqrt(normalised[0] * normalised[0] + normalised[1] * normalised[1]);
        assertThat(norm).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void l2NormaliseHandlesZeroVector() {
        float[] zero = {0f, 0f, 0f};
        assertThatCode(() -> EmbeddingService.l2Normalise(zero)).doesNotThrowAnyException();
    }

    @Test
    void cacheGrowsWithEmbeds() throws SdkException {
        when(sdk.createEmbedding(any())).thenReturn(resp(new float[]{1f, 0f}));
        service.embed("text-a");
        service.embed("text-b");
        assertThat(service.cacheSize()).isEqualTo(2);
    }
}

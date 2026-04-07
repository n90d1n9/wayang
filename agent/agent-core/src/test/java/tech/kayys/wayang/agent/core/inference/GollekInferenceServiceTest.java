package tech.kayys.wayang.agent.core.inference;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.wayang.agent.spi.inference.InferenceRequest;
import tech.kayys.wayang.agent.spi.inference.InferenceResponse;
import tech.kayys.wayang.agent.core.memory.AgentMemoryManager;
import tech.kayys.wayang.agent.core.tool.ToolRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class GollekInferenceServiceTest {

        @InjectMocks
        GollekInferenceService inferenceService;

        @Mock
        GollekSdk gollekClient;

        @Mock
        AgentMemoryManager memoryService;

        @Mock
        ToolRegistry toolRegistry;

        @Test
        void testInferBasic() throws Exception {
                InferenceResponse mockResponse = InferenceResponse.builder()
                                .requestId("test-req-1")
                                .content("response content")
                                .model("model-id")
                                .build();
                Mockito.when(gollekClient.createCompletion(any(InferenceRequest.class))).thenReturn(mockResponse);

                AgentInferenceRequest request = AgentInferenceRequest.builder()
                                .userPrompt("hello")
                                .model("model-id")
                                .build();

                AgentInferenceResponse response = inferenceService.infer(request);

                Assertions.assertEquals("response content", response.getContent());
                Assertions.assertNull(response.getProviderUsed());
        }

        @Test
        void testInferWithMemory() throws Exception {
                InferenceResponse mockResponse = InferenceResponse.builder()
                                .requestId("test-req-2")
                                .content("response content")
                                .model("model-id")
                                .build();
                Mockito.when(gollekClient.createCompletion(any(InferenceRequest.class))).thenReturn(mockResponse);
                Mockito.when(memoryService.retrieveContext(anyString(), anyString(), anyInt()))
                                .thenReturn(Uni.createFrom().item("Previous context"));
                Mockito.when(memoryService.storeMemory(anyString(), anyString(), any()))
                                .thenReturn(Uni.createFrom().item("mem-id"));

                AgentInferenceRequest request = AgentInferenceRequest.builder()
                                .userPrompt("hello")
                                .model("model-id")
                                .useMemory(true)
                                .agentId("agent-1")
                                .build();

                AgentInferenceResponse response = inferenceService.infer(request);

                Assertions.assertEquals("response content", response.getContent());
                Mockito.verify(memoryService).retrieveContext(eq("agent-1"), eq("hello"), anyInt());
                Mockito.verify(gollekClient).createCompletion(any(InferenceRequest.class));
        }

        @Test
        void testInferResolvesProviderAndApiKeyFromContext() throws Exception {
                InferenceResponse mockResponse = InferenceResponse.builder()
                                .requestId("test-req-3")
                                .content("provider-aware response")
                                .model("gemini-2.0-flash")
                                .build();
                Mockito.when(gollekClient.createCompletion(any(InferenceRequest.class))).thenReturn(mockResponse);

                AgentInferenceRequest request = AgentInferenceRequest.builder()
                                .userPrompt("hello")
                                .model("gemini-2.0-flash")
                                .additionalParams(java.util.Map.of(
                                                "context", java.util.Map.of(
                                                                "providerMode", "cloud",
                                                                "cloudProvider", java.util.Map.of(
                                                                                "providerId",
                                                                                "tech.kayys/gemini-provider"),
                                                                "_resolvedCredentials", java.util.Map.of(
                                                                                "gemini-api-key", "secret-123"))))
                                .build();

                inferenceService.infer(request);

                ArgumentCaptor<InferenceRequest> captor = ArgumentCaptor.forClass(InferenceRequest.class);
                Mockito.verify(gollekClient, times(1)).createCompletion(captor.capture());
                InferenceRequest actual = captor.getValue();

                Assertions.assertEquals("secret-123", actual.getApiKey());
                Assertions.assertTrue(actual.getPreferredProvider().isPresent());
                Assertions.assertEquals("tech.kayys/gemini-provider", actual.getPreferredProvider().get());
        }

        @Test
        void testInferUsesRequestPreferredProviderWhenProvided() throws Exception {
                InferenceResponse mockResponse = InferenceResponse.builder()
                                .requestId("test-req-4")
                                .content("preferred response")
                                .model("gpt-4")
                                .build();
                Mockito.when(gollekClient.createCompletion(any(InferenceRequest.class))).thenReturn(mockResponse);

                AgentInferenceRequest request = AgentInferenceRequest.builder()
                                .userPrompt("hello")
                                .model("gpt-4")
                                .preferredProvider("tech.kayys/openai-provider")
                                .additionalParams(java.util.Map.of(
                                                "context", java.util.Map.of(
                                                                "providerMode", "local",
                                                                "localProvider", java.util.Map.of(
                                                                                "providerId",
                                                                                "tech.kayys/ollama-provider"))))
                                .build();

                inferenceService.infer(request);

                ArgumentCaptor<InferenceRequest> captor = ArgumentCaptor.forClass(InferenceRequest.class);
                Mockito.verify(gollekClient, times(1)).createCompletion(captor.capture());
                InferenceRequest actual = captor.getValue();

                Assertions.assertTrue(actual.getPreferredProvider().isPresent());
                Assertions.assertEquals("tech.kayys/openai-provider", actual.getPreferredProvider().get());
        }
}

package tech.kayys.wayang.agent.examples;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.InferenceResponse;
import tech.kayys.wayang.agent.spi.InferenceTypes;

import java.util.List;

public final class ExampleInferenceBackend implements InferenceBackend {

    @Override
    public String name() {
        return "example";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public Uni<InferenceResponse> infer(InferenceRequest request) {
        String prompt = request.messages().stream()
                .filter(message -> "user".equals(message.role()))
                .reduce((first, second) -> second)
                .map(InferenceTypes.ChatMessage::content)
                .orElse("");

        return Uni.createFrom().item(InferenceResponse.builder()
                .responseId(request.requestId() + "-example-response")
                .requestId(request.requestId())
                .model(request.model())
                .content("Example backend handled: " + prompt)
                .usage(InferenceTypes.TokenUsage.of(prompt.length(), 6))
                .durationMs(1)
                .build());
    }

    @Override
    public Multi<InferenceTypes.StreamingChunk> stream(InferenceRequest request) {
        return Multi.createFrom().empty();
    }

    @Override
    public List<InferenceTypes.ProviderInfo> listProviders() {
        return List.of(new InferenceTypes.ProviderInfo(
                "example",
                "Example Inference Backend",
                "example-model",
                true,
                java.util.Map.of()));
    }

    @Override
    public boolean isHealthy() {
        return true;
    }
}

package tech.kayys.wayang.agent.examples;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.core.core.AgentClient;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;

import java.time.Duration;

public final class SimpleAgentRunExample {

    private SimpleAgentRunExample() {
    }

    public static AgentClient createClient() {
        return AgentClient.builder()
                .inferenceBackend(new ExampleInferenceBackend())
                .build();
    }

    public static AgentRequest request(String prompt) {
        return AgentRequest.builder()
                .prompt(prompt)
                .strategy(OrchestrationStrategy.REACT)
                .modelId("example-model")
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    public static Uni<AgentResponse> run(String prompt) {
        return createClient().execute(request(prompt));
    }

    public static void main(String[] args) {
        String prompt = args.length == 0 ? "Summarize the active agent core." : String.join(" ", args);
        AgentResponse response = run(prompt).await().atMost(Duration.ofSeconds(10));
        System.out.println(response.answer());
    }
}

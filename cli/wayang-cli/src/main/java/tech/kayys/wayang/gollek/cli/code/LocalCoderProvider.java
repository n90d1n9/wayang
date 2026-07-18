package tech.kayys.wayang.gollek.cli.code;

import jakarta.enterprise.inject.spi.CDI;
import tech.kayys.wayang.agent.coder.orchestration.CoderOrchestrator;
import tech.kayys.wayang.agent.spi.AgentEvent;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.sdk.provider.ChatMessage;
import tech.kayys.wayang.sdk.provider.Provider;
import tech.kayys.wayang.sdk.provider.StreamEvent;
import tech.kayys.wayang.sdk.provider.ToolSpec;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A provider that delegates to the local CDI Wayang Agent Engine (CoderOrchestrator).
 * It bridges the streaming CLI UI directly to the CDI bean, bypassing HTTP.
 */
public class LocalCoderProvider implements Provider {

    private final String strategy;

    public LocalCoderProvider(String strategy) {
        this.strategy = strategy != null ? strategy : "tdd";
    }

    @Override
    public String id() {
        return "engine-local";
    }

    @Override
    public void streamChat(List<ChatMessage> history, String systemPrompt, List<ToolSpec> tools, double temperature, int maxTokens, Consumer<StreamEvent> onEvent) {
        onEvent.accept(new StreamEvent.TextDelta("[Delegating to Wayang Agent Engine (Local) with strategy: " + strategy + "]\n"));
        onEvent.accept(new StreamEvent.ThinkingDelta("Initializing local execution strategy..."));

        String prompt = history.isEmpty() ? "Hello" : history.get(history.size() - 1).textOnly();
        if (prompt == null || prompt.isBlank()) prompt = "Continue";

        AgentRequest request = AgentRequest.builder()
                .prompt(prompt)
                .parameters(Map.of("strategy", strategy))
                .build();

        try {
            tech.kayys.wayang.agent.core.registry.BackendRegistry.initialize();
            tech.kayys.wayang.agent.spi.InferenceBackend backend = tech.kayys.wayang.agent.core.registry.BackendRegistry.getDefaultInferenceBackend();
            
            java.nio.file.Path workspaceDir = java.nio.file.Path.of(System.getProperty("user.dir"));
            List<tech.kayys.wayang.tools.spi.Tool> toolsFromAdapter = tech.kayys.wayang.gollek.cli.code.WayangCodeSkillAdapter.discoverSkills(workspaceDir);
            
            List<tech.kayys.wayang.agent.spi.AgentSkill> coderSkills = toolsFromAdapter.stream().map(tool -> (tech.kayys.wayang.agent.spi.AgentSkill) new tech.kayys.wayang.agent.spi.AgentSkill() {
                @Override public String id() { return tool.id(); }
                @Override public String name() { return tool.name(); }
                @Override public String description() { return tool.description() != null ? tool.description() : tool.name(); }
                @Override public io.smallrye.mutiny.Uni<java.util.Map<String, Object>> execute(java.util.Map<String, Object> context) {
                    tech.kayys.wayang.tools.spi.ToolResult result = tool.execute(context, tech.kayys.wayang.tools.spi.ToolContext.defaults());
                    if (result.success()) {
                        return io.smallrye.mutiny.Uni.createFrom().item(java.util.Map.of("result", result.data()));
                    } else {
                        return io.smallrye.mutiny.Uni.createFrom().failure(new RuntimeException(result.error()));
                    }
                }
            }).toList();
            
            tech.kayys.wayang.agent.coder.orchestration.strategy.TddExecutionStrategy orchestrator = new tech.kayys.wayang.agent.coder.orchestration.strategy.TddExecutionStrategy();
            
            onEvent.accept(new StreamEvent.ThinkingDelta("\nExecuting strategy locally..."));
            
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            
            orchestrator.stream(request, backend, null, coderSkills).subscribe().with(
                    event -> {
                        if ("FINAL_ANSWER".equals(event.type()) || "LOG".equals(event.type())) {
                            Object content = event.metadata().get("content");
                            if (content == null) content = event.metadata().get("message");
                            if (content != null && !content.toString().isBlank()) {
                                onEvent.accept(new StreamEvent.TextDelta(content.toString() + "\n"));
                            }
                        } else if ("THOUGHT".equals(event.type()) || "OBSERVATION".equals(event.type()) || "ACTION".equals(event.type())) {
                            Object content = event.metadata().get("content");
                            if (content == null) content = event.metadata().get("action");
                            if (content != null) {
                                onEvent.accept(new StreamEvent.ThinkingDelta("[" + event.type() + "] " + content.toString() + "\n"));
                            }
                        }
                    },
                    error -> {
                        onEvent.accept(new StreamEvent.ThinkingEnd());
                        onEvent.accept(new StreamEvent.Error("Local engine failed: " + error.getMessage()));
                        latch.countDown();
                    },
                    () -> {
                        onEvent.accept(new StreamEvent.ThinkingEnd());
                        onEvent.accept(new StreamEvent.MessageStop("end_turn"));
                        latch.countDown();
                    }
            );
            
            latch.await();
        } catch (Exception e) {
            onEvent.accept(new StreamEvent.ThinkingEnd());
            onEvent.accept(new StreamEvent.Error("Failed to communicate with local Agent Engine: " + e.getMessage()));
        }
    }
}

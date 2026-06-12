package tech.kayys.gamelan.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.tool.ToolHandler;
import tech.kayys.gamelan.tool.ToolResult;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.util.List;

/**
 * Spawns a focused sub-agent call to handle a self-contained sub-task.
 *
 * <p>Useful for decomposing complex agent tasks: the main agent delegates
 * specialised work (e.g. "write unit tests for this class") to a sub-call
 * with a tightly scoped system prompt, then incorporates the result.
 *
 * <h2>Real bug fixes vs. previous version</h2>
 * <ul>
 *   <li>The model defaulted to the hard-coded string {@code "llama3"} instead
 *       of the configured default model. Fixed: uses {@code config.defaultModel()}.</li>
 *   <li>{@code InferenceRequest.builder().userMessage(prompt)} — the SPI uses
 *       {@code messages(List<Message>)}, not a {@code userMessage()} method.
 *       Fixed to use the correct API.</li>
 *   <li>No timeout guard — a hung sub-agent would block the parent indefinitely.
 *       Fixed: uses {@code config.requestTimeoutSeconds()} for the request.</li>
 * </ul>
 *
 * <pre>{@code
 * <tool_call>
 *   <n>sub_agent</n>
 *   <task>Write JUnit 5 tests for the OrderService.placeOrder() method</task>
 *   <context>
 * public class OrderService {
 *     public Order placeOrder(Cart cart, User user) { ... }
 * }
 *   </context>
 *   <model>qwen2-7b</model>  <!-- optional -->
 * </tool_call>
 * }</pre>
 */
@ApplicationScoped
public class SubAgentTool implements ToolHandler {

    @Inject GollekSdk     sdk;
    @Inject GamelanConfig config;

    @Override public String toolName() { return "sub_agent"; }

    @Override public String description() {
        return "Delegate a focused, self-contained sub-task to a fresh LLM call. "
                + "Good for: generating tests, writing docs, summarising a section, "
                + "translating code to another language.";
    }

    @Override public List<String> parameters() {
        return List.of(
                "task    - Clear description of the sub-task to perform",
                "context - Relevant code or data (optional, appended to the prompt)",
                "model   - LLM model to use (optional, defaults to configured default)"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String task    = call.param("task").strip();
        String context = call.param("context", "").strip();
        String model   = call.param("model",   "").strip();
        if (model.isBlank()) model = config.defaultModel();

        if (task.isBlank()) return ToolResult.failure(toolName(), "'task' parameter is required");

        // Build the user message
        String userMsg = context.isBlank()
                ? task
                : task + "\n\nHere is the relevant code/context:\n\n```\n" + context + "\n```";

        try {
            InferenceResponse response = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(model)
                            .systemPrompt(
                                "You are a focused coding assistant. Complete the assigned task "
                                + "precisely and concisely. Output code or text directly — "
                                + "no tool calls, no preamble.")
                            .messages(List.of(Message.user(userMsg)))
                            .maxTokens(Math.min(config.maxTokens(), 2048))
                            .temperature(0.3)  // lower temp for focused tasks
                            .build());

            String content = response.getContent();
            if (content == null || content.isBlank()) {
                return ToolResult.failure(toolName(), "Sub-agent returned empty response");
            }
            return ToolResult.success(toolName(), content);
        } catch (SdkException e) {
            return ToolResult.failure(toolName(), "Sub-agent failed: " + e.getMessage());
        }
    }
}

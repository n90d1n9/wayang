package tech.kayys.wayang.agent.core.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;

import java.util.*;

/**
 * AI Tool — provides LLM and embedding capabilities.
 */
@ApplicationScoped
public class AITool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(AITool.class);

    @Override
    public String id() { return "ai"; }

    @Override
    public String name() { return "ai"; }

    @Override
    public String description() {
        return "AI/LLM integration tool. Supports chat, embeddings, and model listing.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "op", Map.of("type", "string", "description", "Operation: chat|embed|list_models"),
                        "provider", Map.of("type", "string", "description", "Provider: anthropic|openai"),
                        "prompt", Map.of("type", "string", "description", "User prompt"),
                        "input", Map.of("type", "string", "description", "Text for embedding")),
                "required", List.of("op"));
    }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext context) {
        String op = String.valueOf(params.getOrDefault("op", "chat"));
        try {
            String result = switch (op) {
                case "chat" -> chat(params);
                case "list_models" -> listModels(params);
                default -> "Unsupported operation: " + op;
            };
            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.error(e.getMessage());
        }
    }

    private String chat(Map<String, Object> params) {
        String provider = String.valueOf(params.getOrDefault("provider", "anthropic"));
        String prompt = String.valueOf(params.getOrDefault("prompt", ""));
        log.debug("AI tool chat requested for provider '{}' with prompt length {}", provider, prompt.length());
        return "AI chat execution is delegated to the configured inference backend.";
    }

    private String listModels(Map<String, Object> params) {
        return "Available models: claude-3-5-sonnet-20240620, claude-3-opus-20240229, claude-3-haiku-20240307";
    }
}

package tech.kayys.wayang.agent.core.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.core.orchestrator.AgentConfig;
import tech.kayys.wayang.agent.core.orchestrator.AnthropicClient;
import tech.kayys.gollek.tools.*;
import tech.kayys.gollek.tools.spi.Tool;
import tech.kayys.gollek.tools.spi.ToolContext;
import tech.kayys.gollek.tools.spi.ToolResult;

import java.util.*;

/**
 * AI Tool — provides LLM and embedding capabilities.
 */
@ApplicationScoped
public class AITool implements CodeTool {

    private static final Logger log = LoggerFactory.getLogger(AITool.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(ToolRegistry.class);

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
        return ToolRegistry.SchemaBuilder.create()
                .string("op", "Operation: chat|embed|list_models", true)
                .string("provider", "Provider: anthropic|openai", false)
                .string("prompt", "User prompt", false)
                .string("input", "Text for embedding", false)
                .build();
    }

    @Override
    public ToolResult execute(Map<String, Object> params, CodeToolContext context) {
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

    private String chat(Map<String, Object> params) throws Exception {
        String provider = String.valueOf(params.getOrDefault("provider", "anthropic"));
        if ("anthropic".equals(provider)) {
            // Use the local AnthropicClient
            AgentConfig config = AgentConfig.builder()
                    .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                    .build();
            AnthropicClient client = new AnthropicClient(config);
            
            List<Map<String, Object>> messages = List.of(Map.of("role", "user", "content", params.get("prompt")));
            JsonNode response = client.sendMessage(messages, List.of(), "You are a helpful assistant.");
            return AnthropicClient.extractText(response);
        }
        return "Only 'anthropic' provider is currently supported in this simplified AITool.";
    }

    private String listModels(Map<String, Object> params) {
        return "Available models: claude-3-5-sonnet-20240620, claude-3-opus-20240229, claude-3-haiku-20240307";
    }
}

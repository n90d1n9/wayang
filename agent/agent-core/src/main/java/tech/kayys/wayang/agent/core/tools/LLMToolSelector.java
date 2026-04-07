package tech.kayys.wayang.agent.core.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.provider.LLMProvider;
import tech.kayys.gollek.spi.provider.ProviderRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Selects tools using an LLM provider to reason about which tools are appropriate.
 * Uses chain-of-thought prompting to generate structured tool selection output.
 */
public class LLMToolSelector implements ToolSelector {

    private static final Logger LOGGER = Logger.getLogger(LLMToolSelector.class);

    private final LLMProvider provider;
    private final ObjectMapper mapper;
    private final ToolSelector fallback;

    public LLMToolSelector(LLMProvider provider) {
        this.provider = provider;
        this.mapper = new ObjectMapper();
        this.fallback = new HeuristicToolSelector();
    }

    public LLMToolSelector(LLMProvider provider, ToolSelector fallback) {
        this.provider = provider;
        this.mapper = new ObjectMapper();
        this.fallback = fallback;
    }

    @Override
    public List<ToolDefinition> selectTools(String query, List<ToolDefinition> availableTools) {
        if (availableTools.isEmpty()) {
            return List.of();
        }

        try {
            // Build tool schema for LLM
            String toolSchema = buildToolSchema(availableTools);

            // Create provider request for tool selection
            String systemPrompt = buildSystemPrompt(toolSchema);
            List<Message> messages = List.of(
                Message.system(systemPrompt),
                Message.user(query)
            );

            ProviderRequest request = new ProviderRequest(
                    UUID.randomUUID().toString(),
                    "default",
                    messages,
                    Map.of("max_tokens", 500),
                    List.of(),
                    null,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of()
            );

            // Call LLM synchronously (blocking)
            InferenceResponse response = provider.infer(request)
                    .await().indefinitely();

            // Parse tool selection from response
            List<ToolDefinition> selected = parseToolSelection(response.getContent(), availableTools);

            if (!selected.isEmpty()) {
                LOGGER.debugf("LLM selected %d tools for query: %s", selected.size(), query);
                return selected;
            }

        } catch (Exception e) {
            LOGGER.warnf(e, "LLM tool selection failed, falling back to heuristic: %s", e.getMessage());
        }

        // Fallback to heuristic if LLM fails
        return fallback.selectTools(query, availableTools);
    }

    /**
     * Build tool schema JSON for LLM.
     */
    private String buildToolSchema(List<ToolDefinition> tools) {
        StringBuilder sb = new StringBuilder("Available tools:\n");
        for (int i = 0; i < tools.size(); i++) {
            ToolDefinition tool = tools.get(i);
            sb.append(i + 1).append(". ").append(tool.name()).append(": ").append(tool.description());
            if (!tool.parameters().isEmpty()) {
                sb.append(" (Parameters: ");
                sb.append(tool.parameters().keySet().stream()
                        .collect(Collectors.joining(", ")));
                sb.append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Build system prompt for tool selection.
     */
    private String buildSystemPrompt(String toolSchema) {
        return """
            You are an AI assistant that selects appropriate tools to answer user queries.
            
            %s
            
            Analyze the user query and determine which tools would be helpful.
            
            Respond ONLY with a JSON object in this exact format:
            {
              "reasoning": "Brief explanation of why these tools were selected",
              "selected_tools": ["tool_name_1", "tool_name_2"]
            }
            
            If no tools are needed, return:
            {
              "reasoning": "No tools needed",
              "selected_tools": []
            }
            """.formatted(toolSchema);
    }

    /**
     * Parse JSON response and extract selected tool names.
     */
    private List<ToolDefinition> parseToolSelection(String response, List<ToolDefinition> availableTools) {
        try {
            // Find JSON in response (LLM might include extra text)
            String json = extractJson(response);
            JsonNode root = mapper.readTree(json);

            if (root.has("selected_tools")) {
                List<String> toolNames = new ArrayList<>();
                JsonNode toolsNode = root.get("selected_tools");

                if (toolsNode.isArray()) {
                    for (JsonNode node : toolsNode) {
                        if (node.isTextual()) {
                            toolNames.add(node.asText());
                        }
                    }
                }

                // Map tool names to definitions
                Map<String, ToolDefinition> toolMap = availableTools.stream()
                        .collect(Collectors.toMap(ToolDefinition::name, t -> t));

                return toolNames.stream()
                        .filter(toolMap::containsKey)
                        .map(toolMap::get)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            LOGGER.debugf(e, "Failed to parse tool selection JSON: %s", response);
        }

        return List.of();
    }

    /**
     * Extract JSON object from response (handles LLM adding extra text).
     */
    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return response;
    }
}

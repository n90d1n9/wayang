package tech.kayys.wayang.agent.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/**
 * HTTP client for the Anthropic Messages API.
 */
public class AnthropicClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);
    private static final String API_URL     = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AgentConfig config;
    private final HttpClient  http;

    public AnthropicClient(AgentConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public JsonNode sendMessage(
            List<Map<String, Object>> messages,
            List<Map<String, Object>> tools,
            String systemPrompt
    ) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model",      config.model());
        body.put("max_tokens", config.maxTokens());
        body.put("system",     systemPrompt);
        body.put("messages",   messages);
        if (!tools.isEmpty()) body.put("tools", tools);

        String json = MAPPER.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", config.apiKey())
                .header("anthropic-version", API_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(180))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Anthropic API error: " + response.statusCode() + " " + response.body());
        }
        return MAPPER.readTree(response.body());
    }

    public static String extractText(JsonNode response) {
        StringBuilder sb = new StringBuilder();
        response.path("content").forEach(block -> {
            if ("text".equals(block.path("type").asText())) {
                sb.append(block.path("text").asText());
            }
        });
        return sb.toString().trim();
    }

    public static List<JsonNode> toolUseBlocks(JsonNode response) {
        List<JsonNode> blocks = new ArrayList<>();
        response.path("content").forEach(block -> {
            if ("tool_use".equals(block.path("type").asText())) {
                blocks.add(block);
            }
        });
        return blocks;
    }

    public static String stopReason(JsonNode response) {
        return response.path("stop_reason").asText();
    }
}

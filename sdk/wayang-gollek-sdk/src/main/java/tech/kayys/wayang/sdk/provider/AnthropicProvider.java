package tech.kayys.wayang.sdk.provider;

import tech.kayys.wayang.sdk.provider.SseReader;
import tech.kayys.wayang.sdk.provider.*;
import tech.kayys.wayang.sdk.json.Json;
import tech.kayys.wayang.sdk.json.JsonValue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/**
 * Talks to the Anthropic Messages API (https://api.anthropic.com/v1/messages)
 * with streaming and tool-use support.
 */
public final class AnthropicProvider implements Provider {

    private static final String API_VERSION = "2023-06-01";

    private final String baseUrl;
    private final String apiKeyEnv;
    private final String apiKey;
    private final String model;
    private final HttpClient http;

    public AnthropicProvider(String baseUrl, String apiKey, String apiKeyEnv, String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.apiKeyEnv = apiKeyEnv;
        this.model = model;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    }

    @Override
    public String id() { return "anthropic"; }

    @Override
    public void streamChat(
            List<ChatMessage> messages,
            String systemPrompt,
            List<ToolSpec> tools,
            double temperature,
            int maxTokens,
            Consumer<StreamEvent> onEvent
    ) throws IOException, InterruptedException {

        if (apiKey == null) {
            onEvent.accept(new StreamEvent.Error(
                    "No API key found for provider 'anthropic'. Set " + apiKeyEnv + "."));
            return;
        }

        JsonValue body = buildRequestBody(messages, systemPrompt, tools, temperature, maxTokens);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .header("content-type", "application/json")
                .header("accept", "text/event-stream")
                .timeout(Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body)))
                .build();

        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() >= 400) {
            String errBody = new String(response.body().readAllBytes());
            onEvent.accept(new StreamEvent.Error("Anthropic API error " + response.statusCode() + ": " + summarizeError(errBody)));
            return;
        }

        StreamState state = new StreamState();
        SseReader.read(response.body(), (event, data) -> handleEvent(event, data, state, onEvent));
    }

    private static String summarizeError(String body) {
        try {
            JsonValue v = Json.parse(body);
            String msg = v.get("error").get("message").asString();
            return msg != null ? msg : body;
        } catch (Exception e) {
            return body;
        }
    }

    /** Tracks in-flight content blocks across SSE events (indexed by Anthropic's content_block index). */
    private static final class StreamState {
        final Map<Integer, String> blockType = new HashMap<>();   // index -> "text" | "tool_use"
        final Map<Integer, String> toolId = new HashMap<>();
        final Map<Integer, StringBuilder> toolJson = new HashMap<>();
    }

    private void handleEvent(String event, String data, StreamState state, Consumer<StreamEvent> onEvent) {
        try {
            JsonValue v = Json.parse(data);
            String type = v.get("type").asString();
            if (type == null) type = event;

            switch (type) {
                case "content_block_start" -> {
                    int index = v.get("index").asInt();
                    JsonValue block = v.get("content_block");
                    String blockType = block.get("type").asString();
                    state.blockType.put(index, blockType);
                    if ("tool_use".equals(blockType)) {
                        String id = block.get("id").asString();
                        String name = block.get("name").asString();
                        state.toolId.put(index, id);
                        state.toolJson.put(index, new StringBuilder());
                        onEvent.accept(new StreamEvent.ToolUseStart(id, name));
                    }
                }
                case "content_block_delta" -> {
                    int index = v.get("index").asInt();
                    JsonValue delta = v.get("delta");
                    String deltaType = delta.get("type").asString();
                    if ("text_delta".equals(deltaType)) {
                        onEvent.accept(new StreamEvent.TextDelta(delta.get("text").asString()));
                    } else if ("input_json_delta".equals(deltaType)) {
                        String partial = delta.get("partial_json").asString("");
                        StringBuilder sb = state.toolJson.get(index);
                        if (sb != null) sb.append(partial);
                        String id = state.toolId.get(index);
                        if (id != null) onEvent.accept(new StreamEvent.ToolUseInputDelta(id, partial));
                    }
                }
                case "content_block_stop" -> {
                    int index = v.get("index").asInt();
                    if ("tool_use".equals(state.blockType.get(index))) {
                        String id = state.toolId.get(index);
                        StringBuilder sb = state.toolJson.get(index);
                        JsonValue parsedInput;
                        try {
                            String jsonText = sb != null ? sb.toString() : "{}";
                            parsedInput = jsonText.isBlank() ? JsonValue.object() : Json.parse(jsonText);
                        } catch (Exception e) {
                            parsedInput = JsonValue.object();
                        }
                        onEvent.accept(new StreamEvent.ToolUseEnd(id, parsedInput));
                    }
                }
                case "message_delta" -> {
                    JsonValue delta = v.get("delta");
                    String stopReason = delta.get("stop_reason").asString();
                    JsonValue usage = v.get("usage");
                    if (!usage.isNull() && usage.has("output_tokens")) {
                        onEvent.accept(new StreamEvent.Usage(0, usage.get("output_tokens").asInt()));
                    }
                    if (stopReason != null) {
                        // Defer MessageStop to the message_stop event; some servers omit it though,
                        // so we stash nothing here -- message_stop always follows in practice.
                    }
                }
                case "message_stop" -> onEvent.accept(new StreamEvent.MessageStop("end_turn"));
                case "error" -> {
                    String msg = v.get("error").get("message").asString("Unknown stream error");
                    onEvent.accept(new StreamEvent.Error(msg));
                }
                default -> { /* ping, message_start: nothing to surface */ }
            }
        } catch (Exception e) {
            onEvent.accept(new StreamEvent.Error("Failed to parse stream event: " + e.getMessage()));
        }
    }

    private JsonValue buildRequestBody(
            List<ChatMessage> messages, String systemPrompt, List<ToolSpec> tools,
            double temperature, int maxTokens) {

        JsonValue body = JsonValue.object();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("stream", true);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }

        JsonValue msgArr = JsonValue.array();
        for (ChatMessage m : messages) {
            msgArr.add(toAnthropicMessage(m));
        }
        body.put("messages", msgArr);

        if (tools != null && !tools.isEmpty()) {
            JsonValue toolArr = JsonValue.array();
            for (ToolSpec t : tools) {
                JsonValue tj = JsonValue.object();
                tj.put("name", t.name());
                tj.put("description", t.description());
                tj.put("input_schema", t.inputSchema());
                toolArr.add(tj);
            }
            body.put("tools", toolArr);
        }
        return body;
    }

    private JsonValue toAnthropicMessage(ChatMessage m) {
        JsonValue msg = JsonValue.object();
        msg.put("role", m.role == ChatMessage.Role.USER ? "user" : "assistant");
        JsonValue contentArr = JsonValue.array();
        for (ContentBlock b : m.content) {
            if (b instanceof ContentBlock.Text t) {
                JsonValue block = JsonValue.object();
                block.put("type", "text");
                block.put("text", t.text());
                contentArr.add(block);
            } else if (b instanceof ContentBlock.ToolUse tu) {
                JsonValue block = JsonValue.object();
                block.put("type", "tool_use");
                block.put("id", tu.id());
                block.put("name", tu.name());
                block.put("input", tu.input());
                contentArr.add(block);
            } else if (b instanceof ContentBlock.ToolResult tr) {
                JsonValue block = JsonValue.object();
                block.put("type", "tool_result");
                block.put("tool_use_id", tr.toolUseId());
                block.put("content", tr.content());
                if (tr.isError()) block.put("is_error", true);
                contentArr.add(block);
            }
        }
        msg.put("content", contentArr);
        return msg;
    }
}

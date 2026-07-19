package tech.kayys.wayang.provider;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

import tech.kayys.wayang.json.Json;
import tech.kayys.wayang.json.JsonValue;

/**
 * Talks to any OpenAI-compatible /v1/chat/completions endpoint with
 * streaming + function-calling (tool use) support. Used for both the
 * official OpenAI API and local backends like Ollama, which expose the
 * same wire format.
 */
public final class OpenAiProvider implements Provider {

    private final String baseUrl;
    private final String apiKeyEnv;
    private final String apiKey;
    private final String model;
    private final HttpClient http;

    public OpenAiProvider(String baseUrl, String apiKey, String apiKeyEnv, String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.apiKeyEnv = apiKeyEnv;
        this.model = model;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    }

    @Override
    public String id() { return "openai"; }

    @Override
    public void streamChat(
            List<ChatMessage> messages,
            String systemPrompt,
            List<ToolSpec> tools,
            double temperature,
            int maxTokens,
            Consumer<StreamEvent> onEvent
    ) throws IOException, InterruptedException {

        JsonValue body = buildRequestBody(messages, systemPrompt, tools, temperature, maxTokens);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("content-type", "application/json")
                .header("accept", "text/event-stream")
                .timeout(Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body)));

        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("authorization", "Bearer " + apiKey);
        }

        HttpResponse<InputStream> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() >= 400) {
            String errBody = new String(response.body().readAllBytes());
            onEvent.accept(new StreamEvent.Error("API error " + response.statusCode() + ": " + summarizeError(errBody)));
            return;
        }

        StreamState state = new StreamState();
        SseReader.read(response.body(), (event, data) -> handleChunk(data, state, onEvent));

        // Flush any tool calls that completed without an explicit finish_reason chunk
        // (defensive; well-behaved servers always send finish_reason).
        if (!state.finished) {
            flushToolCalls(state, onEvent);
            onEvent.accept(new StreamEvent.MessageStop(state.toolCalls.isEmpty() ? "end_turn" : "tool_use"));
        }
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

    private static final class ToolCallAccum {
        String id;
        String name;
        final StringBuilder args = new StringBuilder();
        boolean started = false;
    }

    private static final class StreamState {
        final Map<Integer, ToolCallAccum> toolCalls = new LinkedHashMap<>();
        boolean finished = false;
    }

    private void handleChunk(String data, StreamState state, Consumer<StreamEvent> onEvent) {
        try {
            JsonValue chunk = Json.parse(data);

            JsonValue usage = chunk.get("usage");
            if (!usage.isNull() && (usage.has("completion_tokens") || usage.has("prompt_tokens"))) {
                int in = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
                int out = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : 0;
                onEvent.accept(new StreamEvent.Usage(in, out));
            }

            JsonValue choices = chunk.get("choices");
            if (choices.isNull() || choices.size() == 0) return;
            JsonValue choice = choices.get(0);
            JsonValue delta = choice.get("delta");

            String content = delta.get("content").asString();
            if (content != null && !content.isEmpty()) {
                onEvent.accept(new StreamEvent.TextDelta(content));
            }

            JsonValue toolCalls = delta.get("tool_calls");
            if (!toolCalls.isNull()) {
                for (JsonValue tc : toolCalls.asArray()) {
                    int index = tc.has("index") ? tc.get("index").asInt() : 0;
                    ToolCallAccum acc = state.toolCalls.computeIfAbsent(index, k -> new ToolCallAccum());

                    if (tc.has("id") && !tc.get("id").isNull()) acc.id = tc.get("id").asString();
                    JsonValue fn = tc.get("function");
                    if (!fn.isNull()) {
                        if (fn.has("name") && !fn.get("name").isNull()) acc.name = fn.get("name").asString();
                        if (fn.has("arguments") && !fn.get("arguments").isNull()) {
                            String argDelta = fn.get("arguments").asString();
                            acc.args.append(argDelta);
                            if (!acc.started && acc.id != null && acc.name != null) {
                                acc.started = true;
                                onEvent.accept(new StreamEvent.ToolUseStart(acc.id, acc.name));
                            }
                            if (acc.started) {
                                onEvent.accept(new StreamEvent.ToolUseInputDelta(acc.id, argDelta));
                            }
                        }
                    }
                }
            }

            String finishReason = choice.get("finish_reason").asString();
            if (finishReason != null) {
                state.finished = true;
                flushToolCalls(state, onEvent);
                String normalized = "tool_calls".equals(finishReason) ? "tool_use" : finishReason;
                onEvent.accept(new StreamEvent.MessageStop(normalized));
            }
        } catch (Exception e) {
            onEvent.accept(new StreamEvent.Error("Failed to parse stream chunk: " + e.getMessage()));
        }
    }

    private void flushToolCalls(StreamState state, Consumer<StreamEvent> onEvent) {
        for (ToolCallAccum acc : state.toolCalls.values()) {
            if (acc.id == null) continue;
            JsonValue parsed;
            try {
                String text = acc.args.toString();
                parsed = text.isBlank() ? JsonValue.object() : Json.parse(text);
            } catch (Exception e) {
                parsed = JsonValue.object();
            }
            onEvent.accept(new StreamEvent.ToolUseEnd(acc.id, parsed));
        }
        state.toolCalls.clear();
    }

    private JsonValue buildRequestBody(
            List<ChatMessage> messages, String systemPrompt, List<ToolSpec> tools,
            double temperature, int maxTokens) {

        JsonValue body = JsonValue.object();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("stream", true);

        JsonValue streamOptions = JsonValue.object();
        streamOptions.put("include_usage", true);
        body.put("stream_options", streamOptions);

        JsonValue msgArr = JsonValue.array();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            JsonValue sys = JsonValue.object();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            msgArr.add(sys);
        }
        for (ChatMessage m : messages) {
            appendOpenAiMessages(m, msgArr);
        }
        body.put("messages", msgArr);

        if (tools != null && !tools.isEmpty()) {
            JsonValue toolArr = JsonValue.array();
            for (ToolSpec t : tools) {
                JsonValue fn = JsonValue.object();
                fn.put("name", t.name());
                fn.put("description", t.description());
                fn.put("parameters", t.inputSchema());
                JsonValue tj = JsonValue.object();
                tj.put("type", "function");
                tj.put("function", fn);
                toolArr.add(tj);
            }
            body.put("tools", toolArr);
        }
        return body;
    }

    /**
     * OpenAI's format is flatter than Anthropic's: an assistant turn with
     * text + tool calls is one message with a `tool_calls` array, but each
     * tool *result* must be its own message with role "tool". We preserve
     * block order so any interleaved text/tool-result content still maps
     * to a sane sequence of messages.
     */
    private void appendOpenAiMessages(ChatMessage m, JsonValue msgArr) {
        if (m.role == ChatMessage.Role.ASSISTANT) {
            StringBuilder text = new StringBuilder();
            JsonValue toolCallArr = JsonValue.array();
            for (ContentBlock b : m.content) {
                if (b instanceof ContentBlock.Text t) {
                    if (text.length() > 0) text.append('\n');
                    text.append(t.text());
                } else if (b instanceof ContentBlock.ToolUse tu) {
                    JsonValue fn = JsonValue.object();
                    fn.put("name", tu.name());
                    fn.put("arguments", Json.write(tu.input()));
                    JsonValue call = JsonValue.object();
                    call.put("id", tu.id());
                    call.put("type", "function");
                    call.put("function", fn);
                    toolCallArr.add(call);
                }
            }
            JsonValue msg = JsonValue.object();
            msg.put("role", "assistant");
            msg.put("content", text.length() > 0 ? JsonValue.of(text.toString()) : JsonValue.NULL);
            if (toolCallArr.size() > 0) msg.put("tool_calls", toolCallArr);
            msgArr.add(msg);
        } else {
            StringBuilder text = new StringBuilder();
            for (ContentBlock b : m.content) {
                if (b instanceof ContentBlock.Text t) {
                    if (text.length() > 0) text.append('\n');
                    text.append(t.text());
                } else if (b instanceof ContentBlock.ToolResult tr) {
                    if (text.length() > 0) {
                        JsonValue userMsg = JsonValue.object();
                        userMsg.put("role", "user");
                        userMsg.put("content", text.toString());
                        msgArr.add(userMsg);
                        text.setLength(0);
                    }
                    JsonValue toolMsg = JsonValue.object();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", tr.toolUseId());
                    toolMsg.put("content", tr.content());
                    msgArr.add(toolMsg);
                }
            }
            if (text.length() > 0) {
                JsonValue userMsg = JsonValue.object();
                userMsg.put("role", "user");
                userMsg.put("content", text.toString());
                msgArr.add(userMsg);
            }
        }
    }
}

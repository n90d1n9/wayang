package tech.kayys.wayang.gollek.cli.code;

import tech.kayys.wayang.gollek.sdk.WayangInferenceServiceFactory;
import tech.kayys.wayang.sdk.provider.ChatMessage;
import tech.kayys.wayang.sdk.provider.ContentBlock;
import tech.kayys.wayang.sdk.provider.Provider;
import tech.kayys.wayang.sdk.provider.StreamEvent;
import tech.kayys.wayang.sdk.provider.ToolSpec;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.tool.ToolCall;
import tech.kayys.gollek.spi.tool.ToolDefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * {@link Provider} implementation for the {@code code} command that delegates
 * inference to the Gollek/Wayang engine via {@link tech.kayys.wayang.gollek.sdk.WayangInferenceService}.
 *
 * <p>Streaming chunks from the Gollek reactive pipeline are translated into
 * the normalised {@link StreamEvent} format consumed by the agentic-tui
 * {@link tech.kayys.wayang.tui.agent.Agent}.</p>
 */
public class WayangProvider implements Provider {

    private String modelId;
    private final tech.kayys.wayang.gollek.sdk.WayangInferenceService service;

    public WayangProvider(String modelId) {
        this.modelId = modelId;
        this.service = WayangInferenceServiceFactory.create(
                "You are a helpful coding assistant.", this.modelId);
    }
    
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
    
    public String getModelId() {
        return this.modelId;
    }

    @Override
    public String id() {
        return "wayang";
    }

    @Override
    public void streamChat(
            List<ChatMessage> messages,
            String systemPrompt,
            List<ToolSpec> tools,
            double temperature,
            int maxTokens,
            Consumer<StreamEvent> onEvent) throws IOException, InterruptedException {

        if (this.modelId == null || this.modelId.isBlank()) {
            throw new IllegalStateException("No model selected. Type /models to select a model.");
        }

        CountDownLatch latch = new CountDownLatch(1);
        java.util.Map<String, StringBuilder> toolInputBuffers = new java.util.HashMap<>();
        StringBuilder textBuffer = new StringBuilder();

        List<Message> gollekMessages = toGollekMessages(messages);
        List<ToolDefinition> gollekTools = toToolDefinitions(tools);
        StringBuilder injectedPrompt = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            injectedPrompt.append(systemPrompt);
        } else {
            injectedPrompt.append("You are a helpful coding assistant.");
        }


        String finalSystemPrompt = injectedPrompt.toString();

        service.inferenceStreaming(this.modelId, finalSystemPrompt, gollekMessages, gollekTools, tech.kayys.gollek.sdk.core.ChatParams.of(temperature, maxTokens))
               .subscribe().with(
                        chunk -> {
                            String delta = chunk.delta();
                            if (delta != null && !delta.isEmpty()) {
                                textBuffer.append(delta);
                                String tb = textBuffer.toString();
                                int startIdx = tb.indexOf("<tool_call>");
                                
                                if (startIdx >= 0) {
                                    if (startIdx > 0) {
                                        onEvent.accept(new StreamEvent.TextDelta(tb.substring(0, startIdx)));
                                        tb = tb.substring(startIdx);
                                        textBuffer.setLength(0);
                                        textBuffer.append(tb);
                                    }
                                    
                                    int endIdx = tb.indexOf("</tool_call>");
                                    if (endIdx >= 0) {
                                        String toolJson = tb.substring("<tool_call>".length(), endIdx).trim();
                                        try {
                                            tech.kayys.wayang.sdk.json.JsonValue parsed = tech.kayys.wayang.sdk.json.Json.parse(toolJson);
                                            String tId = java.util.UUID.randomUUID().toString();
                                            String tName = parsed.get("name").asString();
                                            tech.kayys.wayang.sdk.json.JsonValue args = parsed.get("arguments");
                                            
                                            onEvent.accept(new StreamEvent.ToolUseStart(tId, tName));
                                            if (args != null) {
                                                onEvent.accept(new StreamEvent.ToolUseInputDelta(tId, args.toString()));
                                            }
                                            onEvent.accept(new StreamEvent.ToolUseEnd(tId, args != null ? args : tech.kayys.wayang.sdk.json.JsonValue.object()));
                                        } catch (Exception e) {
                                            onEvent.accept(new StreamEvent.TextDelta("\n[WayangProvider] Failed to parse tool call: " + toolJson + "\n"));
                                        }
                                        tb = tb.substring(endIdx + "</tool_call>".length());
                                        textBuffer.setLength(0);
                                        textBuffer.append(tb);
                                    }
                                } else {
                                    boolean endsWithPartial = false;
                                    String tc = "<tool_call>";
                                    for (int i = 1; i <= tc.length(); i++) {
                                        if (tb.endsWith(tc.substring(0, i))) {
                                            endsWithPartial = true;
                                            break;
                                        }
                                    }
                                    if (!endsWithPartial) {
                                        onEvent.accept(new StreamEvent.TextDelta(tb));
                                        textBuffer.setLength(0);
                                    }
                                }
                            }
                            if (chunk.toolCallId() != null) {
                                String tId = chunk.toolCallId();
                                if (chunk.isToolCallStart()) {
                                    onEvent.accept(new StreamEvent.ToolUseStart(tId, chunk.toolName()));
                                    toolInputBuffers.put(tId, new StringBuilder());
                                } else if (chunk.isToolCallDelta()) {
                                    onEvent.accept(new StreamEvent.ToolUseInputDelta(tId, chunk.toolInputDelta()));
                                    if (toolInputBuffers.containsKey(tId)) {
                                        toolInputBuffers.get(tId).append(chunk.toolInputDelta());
                                    }
                                } else if (chunk.isToolCallEnd()) {
                                    StringBuilder sb = toolInputBuffers.remove(tId);
                                    String fullJson = sb != null ? sb.toString() : "{}";
                                    if (fullJson.isBlank()) fullJson = "{}";
                                    try {
                                        tech.kayys.wayang.sdk.json.JsonValue parsed = tech.kayys.wayang.sdk.json.Json.parse(fullJson);
                                        onEvent.accept(new StreamEvent.ToolUseEnd(tId, parsed));
                                    } catch (Exception e) {
                                        System.err.println("[WayangProvider] Failed to parse tool json: " + fullJson);
                                        onEvent.accept(new StreamEvent.ToolUseEnd(tId, tech.kayys.wayang.sdk.json.JsonValue.object()));
                                    }
                                }
                            }
                        },
                        failure -> {
                            System.err.println("[WayangProvider] Streaming failure: " + failure.getMessage());
                            failure.printStackTrace(System.err);
                            System.err.flush();
                            onEvent.accept(new StreamEvent.Error(failure.getMessage()));
                            onEvent.accept(new StreamEvent.MessageStop("error"));
                            latch.countDown();
                        },
                        () -> {
                            if (textBuffer.length() > 0) {
                                onEvent.accept(new StreamEvent.TextDelta(textBuffer.toString()));
                            }
                            onEvent.accept(new StreamEvent.MessageStop("end_turn"));
                            latch.countDown();
                        }
               );

        latch.await();
    }

    private static List<Message> toGollekMessages(List<ChatMessage> history) {
        List<Message> result = new ArrayList<>();
        for (ChatMessage m : history) {
            switch (m.role) {
                case USER -> {
                    List<ContentBlock.ToolResult> toolResults = extractToolResults(m.content);
                    if (!toolResults.isEmpty()) {
                        for (ContentBlock.ToolResult tr : toolResults) {
                            result.add(Message.tool(tr.toolUseId(), tr.content()));
                        }
                    } else {
                        result.add(Message.user(m.textOnly()));
                    }
                }
                case ASSISTANT -> {
                    List<ToolCall> calls = extractToolCalls(m.content);
                    if (calls.isEmpty()) {
                        result.add(Message.assistant(m.textOnly()));
                    } else {
                        result.add(Message.assistantWithToolCalls(m.textOnly(), calls));
                    }
                }
            }
        }
        return result;
    }

    private static List<ToolCall> extractToolCalls(List<ContentBlock> blocks) {
        List<ToolCall> calls = new ArrayList<>();
        for (ContentBlock b : blocks) {
            if (b instanceof ContentBlock.ToolUse tu) {
                Map<String, Object> input = tu.input().asStringObjectMap();
                calls.add(ToolCall.builder()
                    .id(tu.id())
                    .name(tu.name())
                    .arguments(input)
                    .build());
            }
        }
        return calls;
    }

    private static List<ContentBlock.ToolResult> extractToolResults(List<ContentBlock> blocks) {
        List<ContentBlock.ToolResult> results = new ArrayList<>();
        for (ContentBlock b : blocks) {
            if (b instanceof ContentBlock.ToolResult tr) {
                results.add(tr);
            }
        }
        return results;
    }

    private static List<ToolDefinition> toToolDefinitions(List<ToolSpec> specs) {
        if (specs == null) return List.of();
        List<ToolDefinition> defs = new ArrayList<>();
        for (ToolSpec s : specs) {
            defs.add(ToolDefinition.builder()
                .name(s.name())
                .description(s.description())
                .parameters(s.inputSchema())
                .build());
        }
        return defs;
    }
}

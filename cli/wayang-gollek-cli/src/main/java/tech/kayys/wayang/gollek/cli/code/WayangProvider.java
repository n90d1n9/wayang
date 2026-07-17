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
    private String providerId;
    private String apiKey;
    private final tech.kayys.wayang.gollek.sdk.WayangInferenceService service;

    public WayangProvider(String modelId) {
        this(modelId, null, null);
    }

    public WayangProvider(String modelId, String providerId, String apiKey) {
        this.modelId = modelId;
        this.providerId = providerId;
        this.apiKey = apiKey;
        // Inject API key as System property so provider plugins pick it up
        injectApiKey();
        // Configure SDK preferred provider before initialization
        if (providerId != null && !providerId.isBlank()) {
            System.setProperty("gollek.preferred.provider", providerId);
        }
        this.service = WayangInferenceServiceFactory.create(
                "You are a helpful coding assistant.", this.modelId);
        // Set preferred provider on SDK after creation
        try {
            tech.kayys.gollek.sdk.core.GollekSdk sdk = service.getSdk();
            if (sdk != null && providerId != null && !providerId.isBlank()) {
                sdk.setPreferredProvider(providerId);
            }
        } catch (Throwable ignore) {}
    }
    
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
    
    public String getModelId() {
        return this.modelId;
    }

    private void injectApiKey() {
        if (this.providerId != null && !this.providerId.isBlank() && this.apiKey != null && !this.apiKey.isBlank()) {
            System.setProperty(this.providerId + ".api.key", this.apiKey);
        }
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
        boolean[] isThinking = {false};

        List<Message> gollekMessages = toGollekMessages(messages, this.providerId);
        List<ToolDefinition> gollekTools = toToolDefinitions(tools);
        StringBuilder injectedPrompt = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            injectedPrompt.append(systemPrompt);
        } else {
            injectedPrompt.append("You are a helpful coding assistant.");
        }

        ToolPromptAdapter adapter = resolveAdapter(this.providerId);
        adapter.injectTools(injectedPrompt, tools);

        String finalSystemPrompt = injectedPrompt.toString();

        io.smallrye.mutiny.subscription.Cancellable cancellable = service.inferenceStreaming(this.modelId, finalSystemPrompt, gollekMessages, gollekTools, tech.kayys.gollek.sdk.core.ChatParams.of(temperature, maxTokens))
               .subscribe().with(
                        chunk -> {
                            String delta = chunk.delta();
                            if (delta != null && !delta.isEmpty()) {
                                textBuffer.append(delta);
                                boolean processing = true;
                                while (processing && textBuffer.length() > 0) {
                                    processing = false;
                                    String tb = textBuffer.toString();
                                    
                                    if (isThinking[0]) {
                                        int endThink = tb.indexOf("</thought>");
                                        int startTool = tb.indexOf("<tool_call>");
                                        if (startTool >= 0 && (endThink < 0 || startTool < endThink)) {
                                            if (startTool > 0) {
                                                onEvent.accept(new StreamEvent.ThinkingDelta(tb.substring(0, startTool)));
                                            }
                                            onEvent.accept(new StreamEvent.ThinkingEnd());
                                            isThinking[0] = false;
                                            textBuffer.setLength(0);
                                            textBuffer.append(tb.substring(startTool));
                                            processing = true;
                                        } else if (endThink >= 0) {
                                            if (endThink > 0) {
                                                onEvent.accept(new StreamEvent.ThinkingDelta(tb.substring(0, endThink)));
                                            }
                                            onEvent.accept(new StreamEvent.ThinkingEnd());
                                            isThinking[0] = false;
                                            textBuffer.setLength(0);
                                            textBuffer.append(tb.substring(endThink + "</thought>".length()));
                                            processing = true;
                                        } else {
                                            boolean endsWithPartial = false;
                                            String endTag = "</thought>";
                                            for (int i = 1; i <= endTag.length(); i++) {
                                                if (tb.endsWith(endTag.substring(0, i))) {
                                                    endsWithPartial = true;
                                                    break;
                                                }
                                            }
                                            if (!endsWithPartial) {
                                                onEvent.accept(new StreamEvent.ThinkingDelta(tb));
                                                textBuffer.setLength(0);
                                            } else if (tb.length() > endTag.length()) {
                                                int flushLen = tb.length();
                                                for (int i = 1; i <= endTag.length(); i++) {
                                                    if (tb.endsWith(endTag.substring(0, i))) {
                                                        flushLen = tb.length() - i;
                                                        break;
                                                    }
                                                }
                                                onEvent.accept(new StreamEvent.ThinkingDelta(tb.substring(0, flushLen)));
                                                textBuffer.setLength(0);
                                                textBuffer.append(tb.substring(flushLen));
                                            }
                                        }
                                    } else {
                                        int startTool = tb.indexOf("<tool_call>");
                                        int startThink = tb.indexOf("<thought>");
                                        
                                        int firstTag = -1;
                                        boolean isTool = false;
                                        if (startTool >= 0 && startThink >= 0) {
                                            firstTag = Math.min(startTool, startThink);
                                            isTool = (startTool < startThink);
                                        } else if (startTool >= 0) {
                                            firstTag = startTool;
                                            isTool = true;
                                        } else if (startThink >= 0) {
                                            firstTag = startThink;
                                            isTool = false;
                                        }

                                        if (firstTag > 0) {
                                            onEvent.accept(new StreamEvent.TextDelta(tb.substring(0, firstTag)));
                                            textBuffer.setLength(0);
                                            textBuffer.append(tb.substring(firstTag));
                                            processing = true;
                                        } else if (firstTag == 0) {
                                            if (isTool) {
                                                int endIdx = tb.indexOf("</tool_call>");
                                                if (endIdx >= 0) {
                                                    String toolRaw = tb.substring("<tool_call>".length(), endIdx).trim();
                                                    try {
                                                        String tId = java.util.UUID.randomUUID().toString();
                                                        String tName = null;
                                                        tech.kayys.wayang.sdk.json.JsonValue args = null;

                                                        int jsonStart = toolRaw.indexOf('{');
                                                        int jsonEnd = toolRaw.lastIndexOf('}');
                                                        if (jsonStart == 0 && jsonEnd > jsonStart) {
                                                            try {
                                                                tech.kayys.wayang.sdk.json.JsonValue parsed = tech.kayys.wayang.sdk.json.Json.parse(toolRaw);
                                                                tName = parsed.get("name") != null ? parsed.get("name").asString() : null;
                                                                args = parsed.get("arguments");
                                                            } catch (Exception ignored) {}
                                                        }

                                                        if (tName == null && jsonStart > 0) {
                                                            String namePart = toolRaw.substring(0, jsonStart).trim();
                                                            if (namePart.endsWith("(")) namePart = namePart.substring(0, namePart.length() - 1).trim();
                                                            tName = namePart.replaceAll("[^a-zA-Z0-9_]", "");
                                                            String jsonPart = toolRaw.substring(jsonStart, jsonEnd + 1);
                                                            try {
                                                                args = tech.kayys.wayang.sdk.json.Json.parse(jsonPart);
                                                            } catch (Exception ignored) {
                                                                String fixed = jsonPart
                                                                    .replaceAll("'([^']*)'", "\"$1\"")
                                                                    .replaceAll("([{,])\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*:", "$1\"$2\":");
                                                                try {
                                                                    args = tech.kayys.wayang.sdk.json.Json.parse(fixed);
                                                                } catch (Exception ignored2) {
                                                                    args = tech.kayys.wayang.sdk.json.Json.parse("{\"path\": " + com.fasterxml.jackson.databind.node.TextNode.valueOf(jsonPart).toString() + "}");
                                                                }
                                                            }
                                                        }
                                                        
                                                        if (tName == null && !toolRaw.isEmpty()) {
                                                            tName = toolRaw.split("[\\s({]")[0].replaceAll("[^a-zA-Z0-9_]", "");
                                                            args = tech.kayys.wayang.sdk.json.JsonValue.object();
                                                        }

                                                        if (tName != null && !tName.isBlank()) {
                                                            onEvent.accept(new StreamEvent.ToolUseStart(tId, tName));
                                                            if (args != null) {
                                                                onEvent.accept(new StreamEvent.ToolUseInputDelta(tId, args.toString()));
                                                            }
                                                            onEvent.accept(new StreamEvent.ToolUseEnd(tId, args != null ? args : tech.kayys.wayang.sdk.json.JsonValue.object()));
                                                        } else {
                                                            onEvent.accept(new StreamEvent.TextDelta("\n[tool call parse failed: " + toolRaw + "]\n"));
                                                        }
                                                    } catch (Exception e) {
                                                        onEvent.accept(new StreamEvent.TextDelta("\n[WayangProvider] Failed to parse tool call: " + toolRaw + "\n"));
                                                    }
                                                    textBuffer.setLength(0);
                                                    textBuffer.append(tb.substring(endIdx + "</tool_call>".length()));
                                                    processing = true;
                                                }
                                                // If no </tool_call> yet, wait for more data.
                                            } else {
                                                // It's a <thought>
                                                isThinking[0] = true;
                                                textBuffer.setLength(0);
                                                textBuffer.append(tb.substring("<thought>".length()));
                                                processing = true;
                                            }
                                        } else {
                                            // Stream normal text delta
                                            boolean endsWithPartial = false;
                                            for (String tc : new String[]{"<tool_call>", "<thought>"}) {
                                                for (int i = 1; i <= tc.length(); i++) {
                                                    if (tb.endsWith(tc.substring(0, i))) {
                                                        endsWithPartial = true;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (!endsWithPartial) {
                                                onEvent.accept(new StreamEvent.TextDelta(tb));
                                                textBuffer.setLength(0);
                                            } else if (tb.length() > 11) { // 11 is length of <tool_call>
                                                int maxLen = 0;
                                                for (String tc : new String[]{"<tool_call>", "<thought>"}) {
                                                    for (int i = 1; i <= tc.length(); i++) {
                                                        if (tb.endsWith(tc.substring(0, i))) {
                                                            maxLen = Math.max(maxLen, i);
                                                        }
                                                    }
                                                }
                                                onEvent.accept(new StreamEvent.TextDelta(tb.substring(0, tb.length() - maxLen)));
                                                textBuffer.setLength(0);
                                                textBuffer.append(tb.substring(tb.length() - maxLen));
                                            }
                                        }
                                    }
                                }
                            }
                            if (chunk.toolCallId() != null) {
                                String tId = chunk.toolCallId();
                                if (chunk.isToolCallStart()) {
                                    String tName = chunk.toolName();
                                    if (tName != null && !tName.isBlank()) {
                                        onEvent.accept(new StreamEvent.ToolUseStart(tId, tName));
                                        toolInputBuffers.put(tId, new StringBuilder());
                                    }
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
                            String tb = textBuffer.toString();
                            if (tb.contains("<tool_call>")) {
                                int startIdx = tb.indexOf("<tool_call>");
                                String toolJson = tb.substring(startIdx + "<tool_call>".length());
                                int jsonStart = toolJson.indexOf('{');
                                int jsonEnd = toolJson.lastIndexOf('}');
                                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                                    toolJson = toolJson.substring(jsonStart, jsonEnd + 1);
                                    try {
                                        tech.kayys.wayang.sdk.json.JsonValue parsed = tech.kayys.wayang.sdk.json.Json.parse(toolJson);
                                        String tId = java.util.UUID.randomUUID().toString();
                                        String tName = parsed.get("name").asString();
                                        tech.kayys.wayang.sdk.json.JsonValue args = parsed.get("arguments");
                                        
                                        if (startIdx > 0) {
                                            onEvent.accept(new StreamEvent.TextDelta(tb.substring(0, startIdx)));
                                        }
                                        onEvent.accept(new StreamEvent.ToolUseStart(tId, tName));
                                        if (args != null) {
                                            onEvent.accept(new StreamEvent.ToolUseInputDelta(tId, args.toString()));
                                        }
                                        onEvent.accept(new StreamEvent.ToolUseEnd(tId, args != null ? args : tech.kayys.wayang.sdk.json.JsonValue.object()));
                                        textBuffer.setLength(0); // Successfully processed
                                    } catch (Exception ignore) {}
                                }
                            }

                            if (textBuffer.length() > 0) {
                                onEvent.accept(new StreamEvent.TextDelta(textBuffer.toString()));
                            }
                            onEvent.accept(new StreamEvent.MessageStop("end_turn"));
                            latch.countDown();
                        }
               );

        try {
            latch.await();
        } catch (InterruptedException e) {
            if (cancellable != null) {
                cancellable.cancel();
            }
            throw e;
        }
    }

    private static List<Message> toGollekMessages(List<ChatMessage> history, String providerId) {
        boolean useFallback = resolveAdapter(providerId) instanceof XmlFallbackToolAdapter;
        
        List<Message> result = new ArrayList<>();
        for (ChatMessage m : history) {
            switch (m.role) {
                case USER -> {
                    List<ContentBlock.ToolResult> toolResults = extractToolResults(m.content);
                    if (!toolResults.isEmpty()) {
                        if (useFallback) {
                            StringBuilder sb = new StringBuilder();
                            for (ContentBlock.ToolResult tr : toolResults) {
                                sb.append("\n[Tool Result (id: ").append(tr.toolUseId()).append(")]\n");
                                sb.append(tr.content()).append("\n");
                            }
                            result.add(Message.user(sb.toString()));
                        } else {
                            for (ContentBlock.ToolResult tr : toolResults) {
                                result.add(Message.tool(tr.toolUseId(), tr.content()));
                            }
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
                        if (useFallback) {
                            StringBuilder sb = new StringBuilder();
                            if (m.textOnly() != null) {
                                sb.append(m.textOnly());
                            }
                            for (ToolCall tc : calls) {
                                sb.append("\n<tool_call>{\"name\": \"").append(tc.getFunction().getName()).append("\", \"arguments\": ");
                                try {
                                    sb.append(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(tc.getArguments()));
                                } catch (Exception e) {
                                    sb.append(tc.getArguments().toString());
                                }
                                sb.append("}</tool_call>");
                            }
                            result.add(Message.assistant(sb.toString()));
                        } else {
                            result.add(Message.assistantWithToolCalls(m.textOnly(), calls));
                        }
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

    // ─────────────────────────────────────────────────────────────────────────
    // Tool Prompt Abstraction
    // ─────────────────────────────────────────────────────────────────────────

    private interface ToolPromptAdapter {
        void injectTools(StringBuilder prompt, List<ToolSpec> tools);
    }

    private static ToolPromptAdapter resolveAdapter(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return new XmlFallbackToolAdapter();
        }
        String id = providerId.toLowerCase();
        if (id.equals("gollek-subprocess")) {
            return new XmlFallbackToolAdapter();
        }
        if (id.equals("openai") || id.equals("anthropic") || id.equals("gemini") || id.equals("google")) {
            return new NativeToolAdapter();
        }
        return new XmlFallbackToolAdapter();
    }

    private static class NativeToolAdapter implements ToolPromptAdapter {
        @Override
        public void injectTools(StringBuilder prompt, List<ToolSpec> tools) {
            // No-op: Provider supports native tool payloads (e.g. OpenAI, Anthropic, Gemini)
        }
    }

    private static class XmlFallbackToolAdapter implements ToolPromptAdapter {
        @Override
        public void injectTools(StringBuilder prompt, List<ToolSpec> tools) {
            if (tools == null || tools.isEmpty()) return;
            prompt.append("\n\n[TOOL INSTRUCTIONS]\n");
            prompt.append("You have access to the following tools. To call a tool, output ONLY this exact XML on its own line:\n");
            prompt.append("<tool_call>{\"name\": \"TOOL_NAME\", \"arguments\": {\"KEY\": \"VALUE\"}}</tool_call>\n\n");
            prompt.append("IMPORTANT RULES:\n");
            prompt.append("- The JSON inside <tool_call> MUST start with {\"name\": followed by the exact tool name\n");
            prompt.append("- Do NOT write list_dir{...} or list_dir(...) — always use {\"name\": \"list_dir\", \"arguments\": {...}}\n");
            prompt.append("- Always include ALL required parameters in arguments\n");
            prompt.append("- After outputting the tool call, stop. Do not write anything else.\n\n");
            prompt.append("Available tools:\n");
            for (ToolSpec t : tools) {
                prompt.append("- ").append(t.name()).append(": ").append(t.description()).append("\n");
                if (t.inputSchema() != null) {
                    appendParamDocs(prompt, t.inputSchema());
                }
            }
            prompt.append("\nDo NOT wrap the tool call in markdown code blocks. Output the raw `<tool_call>` tag.\n");
        }

        @SuppressWarnings("unchecked")
        private void appendParamDocs(StringBuilder prompt, java.util.Map<String, Object> schema) {
            Object propsObj = schema.get("properties");
            Object reqObj   = schema.get("required");
            if (!(propsObj instanceof java.util.Map)) return;
            java.util.Map<String, Object> props = (java.util.Map<String, Object>) propsObj;
            java.util.Set<String> required = new java.util.HashSet<>();
            if (reqObj instanceof java.util.List) {
                for (Object r : (java.util.List<?>) reqObj) required.add(String.valueOf(r));
            }
            if (props.isEmpty()) return;
            prompt.append("  Parameters:\n");
            for (java.util.Map.Entry<String, Object> e : props.entrySet()) {
                String name = e.getKey();
                String req  = required.contains(name) ? " [required]" : " [optional]";
                String type = "";
                String desc = "";
                if (e.getValue() instanceof java.util.Map) {
                    java.util.Map<String, Object> p = (java.util.Map<String, Object>) e.getValue();
                    type = p.containsKey("type") ? " (" + p.get("type") + ")" : "";
                    desc = p.containsKey("description") ? " — " + p.get("description") : "";
                }
                prompt.append("    - ").append(name).append(req).append(type).append(desc).append("\n");
            }
        }

        @SuppressWarnings("unchecked")
        private java.util.Map<String, Object> stripDescriptions(java.util.Map<String, Object> schema) {
            java.util.Map<String, Object> copy = new java.util.LinkedHashMap<>(schema);
            copy.remove("description");
            for (java.util.Map.Entry<String, Object> entry : copy.entrySet()) {
                if (entry.getValue() instanceof java.util.Map) {
                    entry.setValue(stripDescriptions((java.util.Map<String, Object>) entry.getValue()));
                } else if (entry.getValue() instanceof java.util.List) {
                    java.util.List<Object> newList = new java.util.ArrayList<>();
                    for (Object item : (java.util.List<Object>) entry.getValue()) {
                        if (item instanceof java.util.Map) {
                            newList.add(stripDescriptions((java.util.Map<String, Object>) item));
                        } else {
                            newList.add(item);
                        }
                    }
                    entry.setValue(newList);
                }
            }
            return copy;
        }
    }
}

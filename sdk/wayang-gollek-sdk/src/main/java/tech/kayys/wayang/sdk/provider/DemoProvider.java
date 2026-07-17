package tech.kayys.wayang.sdk.provider;

import tech.kayys.wayang.sdk.provider.*;
import tech.kayys.wayang.sdk.json.JsonValue;
import tech.kayys.wayang.sdk.json.Json;
import java.util.List;
import java.util.function.Consumer;

/**
 * A zero-network, no-API-key provider that simulates a streaming model
 * response. Lets people try the TUI's UX (streaming, tool calls,
 * permission prompts) without any credentials configured, and is handy
 * for local development/testing of the UI layer itself.
 */
public final class DemoProvider implements Provider {

    @Override public String id() { return "demo"; }

    @Override
    public void streamChat(
            List<ChatMessage> messages, String systemPrompt, List<ToolSpec> tools,
            double temperature, int maxTokens, Consumer<StreamEvent> onEvent
    ) throws InterruptedException {

        ChatMessage last = messages.get(messages.size() - 1);
        String userText = last.textOnly();

        // If the last turn was a tool result, just summarize and stop -- avoids infinite tool loops in the demo.
        boolean lastWasToolResult = last.content.stream().anyMatch(b -> b instanceof ContentBlock.ToolResult);
        if (lastWasToolResult) {
            streamText(onEvent, "Got it -- the tool finished. (This is the offline demo provider; " +
                    "configure a real provider in `~/.wayang/config.json` for actual AI responses.)");
            onEvent.accept(new StreamEvent.MessageStop("end_turn"));
            return;
        }

        boolean wantsTool = !tools.isEmpty() && (userText.toLowerCase().contains("list") ||
                userText.toLowerCase().contains("file") || userText.toLowerCase().contains("ls"));
        boolean wantsBash = !tools.isEmpty() && userText.toLowerCase().contains("run");

        if (wantsBash) {
            ToolSpec bash = tools.stream().filter(t -> t.name().equals("bash")).findFirst().orElse(null);
            if (bash != null) {
                streamText(onEvent, "Sure, let me run that command.\n\n");
                String id = "demo-tool-bash";
                onEvent.accept(new StreamEvent.ToolUseStart(id, "bash"));
                JsonValue input = JsonValue.object();
                input.put("command", "echo hello-from-demo-bash-call");
                onEvent.accept(new StreamEvent.ToolUseInputDelta(id, Json.write(input)));
                onEvent.accept(new StreamEvent.ToolUseEnd(id, input));
                onEvent.accept(new StreamEvent.MessageStop("tool_use"));
                return;
            }
        }

        if (wantsTool) {
            ToolSpec listDir = tools.stream().filter(t -> t.name().equals("list_dir")).findFirst().orElse(null);
            if (listDir != null) {
                streamText(onEvent, "Sure, let me check the current directory.\n\n");
                String id = "demo-tool-1";
                onEvent.accept(new StreamEvent.ToolUseStart(id, "list_dir"));
                JsonValue input = JsonValue.object();
                input.put("path", ".");
                String partial = Json.write(input);
                onEvent.accept(new StreamEvent.ToolUseInputDelta(id, partial));
                onEvent.accept(new StreamEvent.ToolUseEnd(id, input));
                onEvent.accept(new StreamEvent.MessageStop("tool_use"));
                return;
            }
        }

        String reply = "This is the **offline demo provider** -- no API key needed.\n\n" +
                "You said:\n> " + userText.replace("\n", "\n> ") + "\n\n" +
                "To talk to a real model, set an API key (e.g. `ANTHROPIC_API_KEY`) and switch " +
                "your active profile's provider in `~/.wayang/config.json`, or run with `--provider anthropic`.\n\n" +
                "Try asking me to `list files` to see a simulated tool call.";
        streamText(onEvent, reply);
        onEvent.accept(new StreamEvent.MessageStop("end_turn"));
    }

    private void streamText(Consumer<StreamEvent> onEvent, String text) throws InterruptedException {
        // Simulate token-by-token streaming with small chunks and a short delay, like a real model.
        int chunkSize = 3;
        for (int i = 0; i < text.length(); i += chunkSize) {
            String chunk = text.substring(i, Math.min(text.length(), i + chunkSize));
            onEvent.accept(new StreamEvent.TextDelta(chunk));
            Thread.sleep(12);
        }
    }
}

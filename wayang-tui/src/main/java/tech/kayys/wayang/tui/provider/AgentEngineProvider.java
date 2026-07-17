package tech.kayys.wayang.tui.provider;

import tech.kayys.wayang.sdk.provider.ChatMessage;
import tech.kayys.wayang.sdk.provider.StreamEvent;
import tech.kayys.wayang.sdk.provider.Provider;
import tech.kayys.wayang.sdk.provider.ToolSpec;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Consumer;

/**
 * A provider that delegates to the remote Wayang Agent Engine (CoderOrchestrator).
 * It bridges the streaming CLI UI to a backend API endpoint.
 */
public class AgentEngineProvider implements Provider {

    private final String baseUrl;
    private final String strategy;
    private final HttpClient httpClient;

    public AgentEngineProvider(String baseUrl, String strategy) {
        this.baseUrl = baseUrl != null ? baseUrl : "http://localhost:8080/api/v1/coder/run";
        this.strategy = strategy != null ? strategy : "tdd";
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String id() {
        return "engine";
    }

    @Override
    public void streamChat(List<ChatMessage> history, String systemPrompt, List<ToolSpec> tools, double temperature, int maxTokens, Consumer<StreamEvent> onEvent) {
        // Send a notification that we are delegating to the backend
        onEvent.accept(new StreamEvent.TextDelta("[Delegating to Wayang Agent Engine with strategy: " + strategy + "]\n"));
        onEvent.accept(new StreamEvent.ThinkingDelta("Initializing remote execution strategy..."));

        // Get the latest prompt from history
        String prompt = history.isEmpty() ? "Hello" : history.get(history.size() - 1).textOnly();
        if (prompt == null || prompt.isBlank()) prompt = "Continue";

        String jsonPayload = """
                {
                  "requestId": "tui-%d",
                  "prompt": %s,
                  "parameters": {
                    "strategy": "%s"
                  }
                }
                """.formatted(System.currentTimeMillis(), escapeJson(prompt), strategy);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        onEvent.accept(new StreamEvent.ThinkingDelta("\nExecuting strategy... this may take some time as it runs in the background."));

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            onEvent.accept(new StreamEvent.ThinkingEnd());

            if (response.statusCode() == 200) {
                // Very naive JSON parsing for now just to extract 'answer'
                String body = response.body();
                String answer = extractJsonValue(body, "\"answer\"");
                onEvent.accept(new StreamEvent.TextDelta(answer != null ? answer.replace("\\n", "\n") : body));
                onEvent.accept(new StreamEvent.MessageStop("end_turn"));
            } else {
                onEvent.accept(new StreamEvent.Error("Remote engine failed with status " + response.statusCode() + ": " + response.body()));
            }
        } catch (Exception e) {
            onEvent.accept(new StreamEvent.ThinkingEnd());
            onEvent.accept(new StreamEvent.Error("Failed to communicate with Agent Engine: " + e.getClass().getName() + " - " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private String escapeJson(String in) {
        return "\"" + in.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private String extractJsonValue(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colonIdx = json.indexOf(":", idx);
        int startQuote = json.indexOf("\"", colonIdx);
        if (startQuote == -1) return null;
        int endQuote = json.indexOf("\"", startQuote + 1);
        
        // Handle escaped quotes
        while (endQuote != -1 && json.charAt(endQuote - 1) == '\\') {
            endQuote = json.indexOf("\"", endQuote + 1);
        }
        
        if (endQuote == -1) return null;
        return json.substring(startQuote + 1, endQuote);
    }
}

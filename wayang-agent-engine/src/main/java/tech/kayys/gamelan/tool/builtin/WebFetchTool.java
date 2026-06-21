package tech.kayys.gamelan.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.tool.ToolHandler;
import tech.kayys.gamelan.tool.ToolResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@ApplicationScoped
class WebFetchTool implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(WebFetchTool.class);
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String toolName() {
        return "web_fetch";
    }

    @Override
    public String description() {
        return "Fetch text content from a URL via HTTP GET request. Converts HTML to basic readable text if possible. Does not execute JavaScript.";
    }

    @Override
    public List<String> parameters() {
        return List.of(
            "url - The URL to fetch"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String url = call.param("url", "");
        if (url.isBlank()) {
            return ToolResult.failure(toolName(), "Missing url parameter");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "WayangAgent/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                return ToolResult.failure(toolName(), "HTTP error: " + response.statusCode());
            }

            String body = response.body();
            // A simple HTML strip since we don't have JSoup
            String text = body.replaceAll("(?is)<script.*?>.*?</script>", "")
                              .replaceAll("(?is)<style.*?>.*?</style>", "")
                              .replaceAll("<[^>]+>", " ")
                              .replaceAll("\\s+", " ")
                              .trim();

            if (text.length() > 20000) {
                text = text.substring(0, 20000) + "\n... (truncated)";
            }

            return ToolResult.success(toolName(), "Fetched URL successfully.\nContent:\n" + text);
        } catch (IllegalArgumentException e) {
            return ToolResult.failure(toolName(), "Invalid URL format: " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            log.warn("Failed to fetch URL {}: {}", url, e.getMessage());
            return ToolResult.failure(toolName(), "Failed to fetch URL: " + e.getMessage());
        }
    }
}

package tech.kayys.gamelan.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.tool.ToolHandler;
import tech.kayys.gamelan.tool.ToolResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP tool — fetch web pages, call REST APIs, download content.
 *
 * <h2>Why this matters</h2>
 * Claude Code, Qwen-Agent, and Cursor can all fetch URLs. Without this, the
 * agent cannot: check API docs, verify a package exists on npm/PyPI/Maven,
 * test an endpoint it just wrote, or scrape structured data.
 *
 * <h2>Safety</h2>
 * <ul>
 *   <li>30-second connect + read timeout — no hanging</li>
 *   <li>Response body capped at 200 KB to protect context window</li>
 *   <li>Redirects followed automatically (up to 5)</li>
 *   <li>User-Agent identifies as Gamelan CLI</li>
 * </ul>
 *
 * <pre>{@code
 * <!-- GET a URL -->
 * <tool_call>
 *   <n>http_get</n>
 *   <url>https://api.github.com/repos/octocat/Hello-World</url>
 * </tool_call>
 *
 * <!-- POST JSON -->
 * <tool_call>
 *   <n>http_post</n>
 *   <url>https://httpbin.org/post</url>
 *   <body>{"key":"value"}</body>
 *   <content_type>application/json</content_type>
 * </tool_call>
 * }</pre>
 */
@ApplicationScoped
public class HttpTool implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(HttpTool.class);

    private static final int     MAX_BODY_BYTES   = 200_000;
    private static final Duration TIMEOUT          = Duration.ofSeconds(30);
    private static final String  USER_AGENT        = "Gamelan-CLI/1.0 (agentic-dev-tool)";

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();

    @Override public String toolName() { return "http_get"; }

    @Override
    public List<String> toolNames() { return List.of("http_get", "http_post", "http_put", "http_delete"); }

    @Override
    public String description() {
        return "Make HTTP requests (GET/POST/PUT/DELETE). Fetch web pages, call REST APIs, "
                + "check endpoints. Response body capped at 200 KB. Timeout: 30s.";
    }

    @Override
    public List<String> parameters() {
        return List.of(
                "url          - Full URL including scheme (https://...)",
                "body         - Request body (for POST/PUT)",
                "content_type - Content-Type header (default: application/json for POST/PUT)",
                "headers      - Extra headers as 'Key: Value' lines (optional)"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String url = call.param("url").strip();
        if (url.isBlank()) return ToolResult.failure(call.name(), "'url' is required");
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return ToolResult.failure(call.name(), "URL must start with http:// or https://");
        }

        try {
            URI uri = URI.create(url);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(uri)
                    .timeout(TIMEOUT)
                    .header("User-Agent", USER_AGENT);

            // Extra headers
            String extraHeaders = call.param("headers", "");
            for (String header : extraHeaders.split("\n")) {
                int colon = header.indexOf(':');
                if (colon > 0) {
                    reqBuilder.header(header.substring(0, colon).strip(),
                                      header.substring(colon + 1).strip());
                }
            }

            // Method dispatch
            String method = call.name().toUpperCase().replace("HTTP_", "");
            HttpRequest req = switch (method) {
                case "POST", "PUT" -> {
                    String body        = call.param("body", "");
                    String contentType = call.param("content_type", "application/json");
                    yield reqBuilder
                            .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                            .header("Content-Type", contentType)
                            .build();
                }
                case "DELETE" -> reqBuilder.DELETE().build();
                default       -> reqBuilder.GET().build();
            };

            long t0 = System.currentTimeMillis();
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            long elapsed = System.currentTimeMillis() - t0;

            int    status = resp.statusCode();
            byte[] body   = resp.body();
            String bodyStr;
            if (body.length > MAX_BODY_BYTES) {
                bodyStr = new String(body, 0, MAX_BODY_BYTES, StandardCharsets.UTF_8)
                        + "\n\n… [truncated: " + body.length + " bytes total]";
            } else {
                bodyStr = new String(body, StandardCharsets.UTF_8);
            }

            String contentType = resp.headers().firstValue("content-type").orElse("unknown");
            String header      = String.format("%s %d  [%dms, %s]\n\n",
                    method, status, elapsed, contentType);

            log.debug("[http] {} {} → {} in {}ms", method, url, status, elapsed);

            if (status >= 200 && status < 300) {
                return ToolResult.success(call.name(), header + bodyStr);
            } else {
                return new ToolResult(call.name(), header + bodyStr, status,
                        "HTTP " + status + " from " + url);
            }

        } catch (IOException e) {
            return ToolResult.failure(call.name(), "Request failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.failure(call.name(), "Request interrupted");
        } catch (Exception e) {
            return ToolResult.failure(call.name(), "Error: " + e.getMessage());
        }
    }
}

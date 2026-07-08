package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Fetches the text content of any URL (HTTP or HTTPS).
 *
 * <p>Returns readable text by stripping common HTML tags so the LLM can
 * consume the page without receiving raw HTML noise. Useful as a follow-up
 * to {@link WebSearchTool} to read the full content of a result page.
 *
 * <p>Hard-capped at {@code MAX_CHARS} characters to keep context size
 * manageable. Use {@code start_char} / {@code max_chars} to page through
 * longer documents.
 */
public final class FetchUrlTool implements Tool {

    private static final int DEFAULT_MAX_CHARS = 8_000;
    private static final int HARD_CAP_CHARS    = 32_000;
    private static final int CONNECT_TIMEOUT   = 10_000;
    private static final int READ_TIMEOUT      = 15_000;

    @Override public String id()   { return "fetch_url"; }
    @Override public String name() { return "fetch_url"; }

    @Override
    public String description() {
        return "Fetch the text content of any HTTP/HTTPS URL. " +
               "Returns readable text by stripping most HTML tags. " +
               "Useful for reading documentation, blog posts, API references, or any web page. " +
               "Use 'max_chars' to control how much text is returned (default 8000, max 32000). " +
               "Use 'start_char' to paginate through longer pages.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "url",        Schema.string("The full URL (http:// or https://) to fetch."),
                "max_chars",  Schema.integer("Maximum number of characters to return (1–32000, default 8000)."),
                "start_char", Schema.integer("Character offset to start reading from (default 0).")
        ), "url");
    }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext context) {
        String urlStr = (String) params.get("url");
        if (urlStr == null || urlStr.isBlank()) {
            return ToolResult.error("'url' parameter is required.");
        }
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            return ToolResult.error("URL must start with http:// or https://");
        }

        int maxChars  = DEFAULT_MAX_CHARS;
        int startChar = 0;
        if (params.containsKey("max_chars")) {
            maxChars = Math.min(HARD_CAP_CHARS, Math.max(1, ((Number) params.get("max_chars")).intValue()));
        }
        if (params.containsKey("start_char")) {
            startChar = Math.max(0, ((Number) params.get("start_char")).intValue());
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "Wayang-AI-Agent/1.0");
            conn.setRequestProperty("Accept", "text/html,text/plain,application/xhtml+xml,*/*");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.connect();

            int status = conn.getResponseCode();
            if (status >= 400) {
                return ToolResult.error("HTTP " + status + " fetching: " + urlStr);
            }

            String raw;
            try (InputStream is = conn.getInputStream()) {
                raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            String contentType = conn.getContentType();
            String text = (contentType != null && contentType.contains("text/html"))
                    ? stripHtml(raw)
                    : raw;

            // Apply pagination
            if (startChar >= text.length()) {
                return ToolResult.error("start_char (" + startChar + ") is beyond the document length (" + text.length() + " chars).");
            }
            int endChar = Math.min(text.length(), startChar + maxChars);
            String slice = text.substring(startChar, endChar);

            boolean truncated = endChar < text.length();
            String header = "URL: " + urlStr + "\n" +
                    "Characters: " + (startChar + 1) + "–" + endChar + " of " + text.length() + "\n" +
                    (truncated ? "(truncated — use start_char=" + endChar + " to continue)\n" : "") +
                    "=".repeat(60) + "\n\n";

            return ToolResult.success(header + slice);

        } catch (java.net.UnknownHostException e) {
            return ToolResult.error("Cannot resolve host – no internet access or invalid hostname: " + e.getMessage());
        } catch (Exception e) {
            return ToolResult.error("Failed to fetch URL: " + e.getMessage());
        }
    }

    /** Minimal HTML → plain-text stripper. */
    private static String stripHtml(String html) {
        // Remove <script> and <style> blocks entirely
        String s = html
                .replaceAll("(?si)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?si)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?si)<head[^>]*>.*?</head>", " ");
        // Replace block-level elements with newlines
        s = s.replaceAll("(?i)<(br|p|div|li|tr|h[1-6]|blockquote|pre)[^>]*>", "\n");
        // Strip all remaining tags
        s = s.replaceAll("<[^>]+>", "");
        // Decode common HTML entities
        s = s.replace("&amp;",  "&")
             .replace("&lt;",   "<")
             .replace("&gt;",   ">")
             .replace("&quot;", "\"")
             .replace("&apos;", "'")
             .replace("&nbsp;", " ")
             .replace("&#39;",  "'");
        // Collapse excessive whitespace
        s = s.replaceAll("[ \\t]+", " ")
             .replaceAll("\\n{3,}", "\n\n");
        return s.strip();
    }
}

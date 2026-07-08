package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Searches the web using DuckDuckGo.
 *
 * <h3>Strategy</h3>
 * <ol>
 *   <li>Tries the DuckDuckGo Instant Answer <em>JSON API</em>
 *       ({@code api.duckduckgo.com}) — no API key required.</li>
 *   <li>On any SSL/network failure, automatically falls back to the
 *       DuckDuckGo <em>HTML search</em> endpoint
 *       ({@code html.duckduckgo.com/html/}) and parses the HTML results.</li>
 * </ol>
 *
 * <h3>SSL override (development only)</h3>
 * Set {@code -Dwayang.web_search.disable_ssl_verify=true} on the JVM to
 * bypass certificate validation when a self-signed or expired cert is
 * encountered. <strong>Do not use in production.</strong>
 *
 * <p>For deeper results, combine with {@link FetchUrlTool} to read the
 * full content of any URL in the result list.
 */
public final class WebSearchTool implements Tool {

    private static final String DDG_JSON_API  = "https://api.duckduckgo.com/";
    private static final String DDG_HTML_URL  = "https://html.duckduckgo.com/html/";
    private static final int    MAX_RESULTS   = 20;
    private static final int    CONNECT_TIMEOUT = 10_000;
    private static final int    READ_TIMEOUT    = 15_000;

    @Override public String id()   { return "web_search"; }
    @Override public String name() { return "web_search"; }

    @Override
    public String description() {
        return "Search the internet using DuckDuckGo and return relevant results. " +
               "Returns an instant answer (if available), an abstract/summary, and a list of " +
               "result snippets with titles and URLs. " +
               "Use 'num_results' to control how many results are returned (default 10, max 20). " +
               "Combine with the 'fetch_url' tool to read the full content of any result page.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "query",       Schema.string("The search query string."),
                "num_results", Schema.integer("Maximum number of results to return (1–20, default 10).")
        ), "query");
    }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext context) {
        String query = (String) params.get("query");
        if (query == null || query.isBlank()) {
            return ToolResult.error("'query' parameter is required.");
        }
        int numResults = 10;
        if (params.containsKey("num_results")) {
            numResults = Math.min(MAX_RESULTS, Math.max(1, ((Number) params.get("num_results")).intValue()));
        }

        String encoded;
        try {
            encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ToolResult.error("Failed to encode query: " + e.getMessage());
        }

        // Strategy 1: JSON API
        try {
            String url = DDG_JSON_API + "?q=" + encoded + "&format=json&no_html=1&skip_disambig=1";
            String json = fetchText(url, "application/json");
            ToolResult r = parseJson(query, json, numResults);
            // If JSON API returned no useful content, fall through to HTML fallback
            if (!r.output().map(s -> s.contains("No results found")).orElse(true)) return r;
        } catch (Exception jsonEx) {
            // Fall through to HTML strategy
        }

        // Strategy 2: HTML endpoint (more robust, works even when JSON API has cert issues)
        try {
            String url = DDG_HTML_URL + "?q=" + encoded + "&kl=us-en";
            String html = fetchText(url, "text/html");
            return parseHtml(query, html, numResults);
        } catch (java.net.UnknownHostException e) {
            return ToolResult.error("No internet access or DNS resolution failed: " + e.getMessage());
        } catch (Exception e) {
            return ToolResult.error("Web search failed: " + e.getMessage() +
                    "\nTip: if this is an SSL issue, restart Wayang with " +
                    "-Dwayang.web_search.disable_ssl_verify=true");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP helper
    // ─────────────────────────────────────────────────────────────────────────

    private static String fetchText(String urlStr, String acceptHeader) throws Exception {
        HttpURLConnection conn;

        if (Boolean.getBoolean("wayang.web_search.disable_ssl_verify")
                && urlStr.startsWith("https://")) {
            conn = openTrustAllConnection(urlStr);
        } else {
            conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        }

        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (compatible; Wayang-AI-Agent/1.0)");
        conn.setRequestProperty("Accept", acceptHeader);
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.connect();

        int status = conn.getResponseCode();
        if (status >= 400) throw new RuntimeException("HTTP " + status + " from " + urlStr);

        try (InputStream is = conn.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Opens a connection with SSL verification disabled (dev/debug only). */
    @SuppressWarnings("TrustAllX509TrustManager")
    private static HttpURLConnection openTrustAllConnection(String urlStr) throws Exception {
        TrustManager[] tm = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] c, String a) {}
            public void checkServerTrusted(X509Certificate[] c, String a) {}
        }};
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, tm, new SecureRandom());
        HttpsURLConnection conn = (HttpsURLConnection) URI.create(urlStr).toURL().openConnection();
        conn.setSSLSocketFactory(sc.getSocketFactory());
        conn.setHostnameVerifier((h, s) -> true);
        return conn;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON API parser
    // ─────────────────────────────────────────────────────────────────────────

    private static ToolResult parseJson(String query, String json, int numResults) {
        StringBuilder sb = new StringBuilder();
        sb.append("Search results for: ").append(query).append("\n");
        sb.append("=".repeat(60)).append("\n\n");

        String instantAnswer = extractJsonString(json, "Answer");
        if (!instantAnswer.isEmpty()) {
            sb.append("⚡ Instant Answer:\n").append(instantAnswer).append("\n\n");
        }

        String abstractText = extractJsonString(json, "AbstractText");
        String abstractURL  = extractJsonString(json, "AbstractURL");
        if (!abstractText.isEmpty()) {
            sb.append("📄 Summary:\n").append(abstractText).append("\n");
            if (!abstractURL.isEmpty()) sb.append("   Source: ").append(abstractURL).append("\n");
            sb.append("\n");
        }

        String definition    = extractJsonString(json, "Definition");
        String definitionURL = extractJsonString(json, "DefinitionURL");
        if (!definition.isEmpty()) {
            sb.append("📖 Definition:\n").append(definition).append("\n");
            if (!definitionURL.isEmpty()) sb.append("   Source: ").append(definitionURL).append("\n");
            sb.append("\n");
        }

        List<String[]> topics = extractRelatedTopics(json, numResults);
        if (!topics.isEmpty()) {
            sb.append("🔗 Related Topics (").append(topics.size()).append("):\n");
            for (int i = 0; i < topics.size(); i++) {
                String[] t = topics.get(i);
                sb.append(String.format("%2d. %s%n", i + 1, t[0]));
                if (t[1] != null && !t[1].isEmpty()) sb.append("    URL: ").append(t[1]).append("\n");
                sb.append("\n");
            }
        }

        if (instantAnswer.isEmpty() && abstractText.isEmpty() && definition.isEmpty() && topics.isEmpty()) {
            return ToolResult.success("No results found. Try a different or more specific query.\n");
        }
        return ToolResult.success(sb.toString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTML parser (fallback)
    // ─────────────────────────────────────────────────────────────────────────

    private static ToolResult parseHtml(String query, String html, int numResults) {
        StringBuilder sb = new StringBuilder();
        sb.append("Search results for: ").append(query).append("\n");
        sb.append("=".repeat(60)).append("\n\n");

        List<String[]> results = extractHtmlResults(html, numResults);
        if (results.isEmpty()) {
            return ToolResult.success("No results found. Try a different or more specific query.\n");
        }

        sb.append("🔍 Web Results (").append(results.size()).append("):\n\n");
        for (int i = 0; i < results.size(); i++) {
            String[] r = results.get(i);
            sb.append(String.format("%2d. %s%n", i + 1, r[0]));      // title
            if (r[1] != null && !r[1].isEmpty()) {
                sb.append("    Snippet: ").append(r[1]).append("\n"); // snippet
            }
            if (r[2] != null && !r[2].isEmpty()) {
                sb.append("    URL: ").append(r[2]).append("\n");     // url
            }
            sb.append("\n");
        }
        return ToolResult.success(sb.toString());
    }

    /**
     * Extracts result entries from DuckDuckGo HTML response.
     * Looks for {@code <div class="result__body">} blocks.
     */
    private static List<String[]> extractHtmlResults(String html, int max) {
        List<String[]> results = new ArrayList<>();

        // DuckDuckGo HTML results are inside <div class="result__body"> blocks
        String[] markers = {"result__body", "result--web"};
        String marker = html.contains("result__body") ? "result__body" : "result--web";

        int pos = 0;
        while (results.size() < max) {
            int blockStart = html.indexOf(marker, pos);
            if (blockStart < 0) break;

            // Find the enclosing div end (simple heuristic: next </div> after the link block)
            int blockEnd = html.indexOf("</div>", blockStart + marker.length());
            if (blockEnd < 0) break;
            // Extend to capture snippet which may be in sibling divs
            int snippetEnd = html.indexOf("</div>", blockEnd + 1);
            if (snippetEnd > 0 && snippetEnd - blockStart < 2000) blockEnd = snippetEnd;

            String block = html.substring(blockStart, Math.min(blockEnd + 6, html.length()));

            String title   = extractHtmlTag(block, "result__title", "result__a");
            String snippet = extractHtmlTag(block, "result__snippet", "result__description");
            String url     = extractHref(block);

            title   = decodeEntities(stripTags(title)).strip();
            snippet = decodeEntities(stripTags(snippet)).strip();
            url     = cleanDdgRedirect(url);

            if (!title.isEmpty() || !url.isEmpty()) {
                results.add(new String[]{title, snippet, url});
            }
            pos = blockStart + marker.length();
        }
        return results;
    }

    private static String extractHtmlTag(String block, String... classNames) {
        for (String cls : classNames) {
            // match class="..." containing cls
            int idx = block.indexOf("class=\"" + cls + "\"");
            if (idx < 0) idx = block.indexOf("class=\"" + cls + " ");
            if (idx < 0) continue;
            int tagOpen = block.lastIndexOf("<", idx);
            if (tagOpen < 0) continue;
            int tagClose = block.indexOf(">", idx);
            if (tagClose < 0) continue;
            int endIdx = block.indexOf("<", tagClose + 1);
            if (endIdx < 0) endIdx = block.length();
            return block.substring(tagClose + 1, endIdx);
        }
        return "";
    }

    private static String extractHref(String block) {
        int idx = block.indexOf("href=\"");
        if (idx < 0) return "";
        int start = idx + 6;
        int end = block.indexOf("\"", start);
        return end > start ? block.substring(start, end) : "";
    }

    /** DuckDuckGo wraps URLs in redirect links — extract the real URL. */
    private static String cleanDdgRedirect(String url) {
        if (url == null) return "";
        // Pattern: //duckduckgo.com/l/?uddg=<encoded-url>&...
        int uddg = url.indexOf("uddg=");
        if (uddg >= 0) {
            String encoded = url.substring(uddg + 5);
            int amp = encoded.indexOf('&');
            if (amp > 0) encoded = encoded.substring(0, amp);
            try {
                return java.net.URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            } catch (Exception ignore) {}
        }
        return url;
    }

    private static String stripTags(String s) {
        return s == null ? "" : s.replaceAll("<[^>]+>", "");
    }

    private static String decodeEntities(String s) {
        return s == null ? "" : s
                .replace("&amp;",  "&").replace("&lt;",   "<").replace("&gt;",  ">")
                .replace("&quot;", "\"").replace("&apos;", "'").replace("&nbsp;", " ")
                .replace("&#39;",  "'").replaceAll("\\s+", " ");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON string helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int idx = json.indexOf(searchKey);
        if (idx < 0) return "";
        int start = idx + searchKey.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') break;
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default  -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static List<String[]> extractRelatedTopics(String json, int max) {
        List<String[]> results = new ArrayList<>();
        int topicsStart = json.indexOf("\"RelatedTopics\":[");
        if (topicsStart < 0) return results;
        topicsStart += "\"RelatedTopics\":[".length();

        int depth = 0, objStart = -1;
        for (int i = topicsStart; i < json.length() && results.size() < max; i++) {
            char c = json.charAt(i);
            if      (c == '{') { if (depth++ == 0) objStart = i; }
            else if (c == '}') {
                if (--depth == 0 && objStart >= 0) {
                    String obj  = json.substring(objStart, i + 1);
                    String text = extractJsonString(obj, "Text");
                    String url  = extractJsonString(obj, "FirstURL");
                    if (!text.isEmpty()) results.add(new String[]{text, url});
                    objStart = -1;
                }
            } else if (c == ']' && depth == 0) break;
        }
        return results;
    }
}
